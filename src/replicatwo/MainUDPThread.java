package replicatwo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;

import org.omg.CORBA.ORB;

class ORBThread extends Thread
{
	private ORB orb;

	protected ORBThread(ORB pOrb)
	{
		orb = pOrb;
	}

	@Override
	public void run()
	{
		orb.run();
	}
}

class ReplicaManagerListenUDPThread extends Thread
{
	private int aPort;
	private boolean bCrashed;
	private boolean bShouldRestart;
	private DatagramSocket aDatagramSocket;
	private DatagramPacket requestFromReplicaManager;
	private byte [] buffer;
	private String [] messageArray;
	
	private final String UDP_PARSER = "/";
	private int UDP_BUFFER_SIZE = 1200;
	private static final String RM_NAME = "RM";

	private enum ACTION_TO_PERFORM {
		  PLAYER_CREATE_ACCOUNT,
		  PLAYER_SIGN_IN,
		  PLAYER_SIGN_OUT,
		  PLAYER_TRANSFER_ACCOUNT,
		  ADMIN_SIGN_IN,
		  ADMIN_SIGN_OUT,
		  ADMIN_GET_PLAYER_STATUS, 
		  ADMIN_SUSPEND_PLAYER_ACCOUNT,
		  RESTART_REPLICA
	}
	
	protected ReplicaManagerListenUDPThread(int pPort) throws SocketException
	{
		aPort = pPort;
		bCrashed = false;
		bShouldRestart = false;
		aDatagramSocket = new DatagramSocket(aPort);
		buffer = new byte [UDP_BUFFER_SIZE];
	}
	
	protected boolean hasCrashed()
	{
		return bCrashed;
	}
	
	protected boolean shouldRestart()
	{
		return bShouldRestart;
	}
	
	protected void resetShouldRestart()
	{
		bShouldRestart = false;
	}
	
	@Override
	public void run ()
	{
		while(true)
			handleCommunication();
	}
	
	/* Handles communication with the replica manager */
	public void handleCommunication()
	{
		try
		{
			requestFromReplicaManager = new DatagramPacket(buffer, buffer.length);
			aDatagramSocket.receive(requestFromReplicaManager);
			messageArray = (new String(requestFromReplicaManager.getData())).split(UDP_PARSER);
			if(messageArray[0].equals(RM_NAME))
			{
				messageArray[1] = messageArray[1].trim();
				if(messageArray[1].equals(ACTION_TO_PERFORM.RESTART_REPLICA.name()))
				{
					bShouldRestart = true;
				}
			}
		}
		catch (IOException e)
		{
			aDatagramSocket.close();
			bCrashed = true;
		}
	}
}

class MainUDPThread extends Thread
{
	
	private final String UDP_PARSER = "/";
	private int UDP_BUFFER_SIZE = 1200;

	private enum ACTION_TO_PERFORM {
		  PLAYER_CREATE_ACCOUNT,
		  PLAYER_SIGN_IN,
		  PLAYER_SIGN_OUT,
		  PLAYER_TRANSFER_ACCOUNT,
		  ADMIN_SIGN_IN,
		  ADMIN_SIGN_OUT,
		  ADMIN_GET_PLAYER_STATUS, 
		  ADMIN_SUSPEND_PLAYER_ACCOUNT,
		  RESTART_REPLICA
	}
	private static MainUDPThread replicatwo;
	//protected Logger aLog;
	private GameServer aInterfaceIDL;
	private GameServerServant aNAGameServer;
	private GameServerServant aEUGameServer;
	private GameServerServant aASGameServer;
	private Thread aNAThread;
	private Thread aEUThread;
	private Thread aASThread;
	// For receiving from replica leader multicast
	private static MulticastSocket aMulticastSocket;
	private DatagramPacket requestFromLeaderPacket;
	private byte [] buffer;
	private String [] messageArray;
	// For sending replies to replica leader UDP
	private DatagramSocket aSendSocket;
	private DatagramPacket replyToLeaderPacket;
	private String data;
	// For receiving from replica manager UDP
	protected ReplicaManagerListenUDPThread replicaManagerListener;
	
	protected static int UDP_PORT_REPLICA_B = 3000;
	
	protected static String RM_NAME = "RM";
	protected static String LR_NAME = "LR";
	protected static String RB_NAME = "RB";
	protected static String UDP_END_PARSE = "$";
	protected static String RA_NA_NAME = "NA";
	protected static String RA_EU_NAME = "EU";
	protected static String RA_AS_NAME = "AS";
	protected static int UDP_PORT_REPLICA_LEAD = 4000;
	
	protected static int UDP_PORT_REPLICA_LEAD_MULTICAST = 4446;
	
	protected static String UDP_ADDR_REPLICA_COMMUNICATION_MULTICAST = "224.0.0.2";
	
	protected static String GeoLocationOfGameServerNA = "132";
	protected static String GeoLocationOfGameServerEU = "93";
	protected static String GeoLocationOfGameServerAS = "182";
	
	// Main method which runs the UDP thread for replica A
	public static void main(String[] args) throws InterruptedException 
	{
		String[] defaultArgs = {};
		replicatwo = new MainUDPThread(UDP_PORT_REPLICA_B, defaultArgs);
		while (true)
		{
			if(replicatwo.replicaManagerListener.shouldRestart())
			{
				System.out.println("Received RM Command to restart");
				replicatwo.stopServers();
				replicatwo.startServers();
				replicatwo.replicaManagerListener.resetShouldRestart();
			}
			if(replicatwo.replicaManagerListener.hasCrashed())
			{
				System.out.println("Crash detected in ReplicaA Replica Manager UDP Thread, restarting UDP");
				try {
					replicatwo.replicaManagerListener = new ReplicaManagerListenUDPThread(UDP_PORT_REPLICA_B);
				} catch (SocketException e) {
					System.out.println("ReplicaA Replica Manager creating failed");
				}
				replicatwo.replicaManagerListener.start();
			}
			
			Thread.sleep(200);
		}
	}
	
	private MainUDPThread(int pPort, String[] pArgs)
	{
		buffer = new byte [UDP_BUFFER_SIZE];
		try {
			aMulticastSocket = new MulticastSocket(UDP_PORT_REPLICA_LEAD_MULTICAST);
			aMulticastSocket.joinGroup(InetAddress.getByName(UDP_ADDR_REPLICA_COMMUNICATION_MULTICAST));
			aSendSocket = new DatagramSocket();
			replicaManagerListener = new ReplicaManagerListenUDPThread(UDP_PORT_REPLICA_B);
			// Start the UDP communication for replicatwo
			System.out.println("UDP Running");
			replicaManagerListener.start();
			start();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("UDP Socket creation failed");
		}
	}
	
	/* sets the interfaceIDL reference based on the Geo location in the given pIPAddress 
	 * returns true if successful */
	private boolean setORBreference(String pIPAddress) throws IOException
	{
		ORB orb = ORB.init(new String [1], null);
		BufferedReader bufferedReader;
		// Get the reference to the CORBA objects from the file
		if(pIPAddress.length() >= 3 && pIPAddress.substring(0,3).equals(GeoLocationOfGameServerNA))
		{
			bufferedReader = new BufferedReader(new FileReader(RA_NA_NAME + "_BIOR.txt"));
		}
		else if(pIPAddress.length() >= 2 && pIPAddress.substring(0,2).equals(GeoLocationOfGameServerEU))
		{
			bufferedReader = new BufferedReader(new FileReader(RA_EU_NAME + "_BIOR.txt"));
		}
		else if(pIPAddress.length() >= 3 && pIPAddress.substring(0,3).equals(GeoLocationOfGameServerAS))
		{
			bufferedReader = new BufferedReader(new FileReader(RA_AS_NAME + "_BIOR.txt"));
		}
		else
		{
			System.out.println("Invalid GeoLocation");
			return false;
		}
		String stringORB = bufferedReader.readLine();
		bufferedReader.close();
		// Transform the reference string to CORBA object
		org.omg.CORBA.Object reference_CORBA = orb.string_to_object(stringORB);
		aInterfaceIDL = GameServerHelper.narrow(reference_CORBA);
		
		orb = null;
		stringORB = null;
		bufferedReader = null;
		
		return true;
	}
	
	/* Creates and starts the servers */
	protected boolean startServers()
	{
		try
		{
			// Create Game Servers within each thread
			aNAGameServer = new GameServerServant(RA_NA_NAME, new ArrayList<>(Arrays.asList(8990,8991)),8989);
			aEUGameServer = new GameServerServant(RA_EU_NAME, new ArrayList<>(Arrays.asList(8989,8991)),8990);
			aASGameServer = new GameServerServant(RA_AS_NAME, new ArrayList<>(Arrays.asList(8989,8990)),8991);
			
			// Start the Threads running each runnable Game Server
			aNAThread = new Thread(aNAGameServer);
			aEUThread = new Thread(aEUGameServer);
			aASThread = new Thread(aASGameServer);
			aNAThread.start();
			aEUThread.start();
			aASThread.start();
			System.out.println("Restarted All Servers");
		}
		catch(Exception e)
		{
			System.out.println("Error starting the servers: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	/* Stops the servers and clears their resources */
	protected boolean stopServers()
	{
		try
		{
			aNAThread = null;
			aEUThread = null;
			aASThread = null;
			if(aNAGameServer != null)
				aNAGameServer.freeServerResources();
			if(aEUGameServer != null)
				aEUGameServer.freeServerResources();
			if(aASGameServer != null)
				aASGameServer.freeServerResources();
			aNAGameServer = null;
			aEUGameServer = null;
			aASGameServer = null;
			System.out.println("Stopped All Servers");
		}
		catch(Exception e)
		{
			System.out.println("Error stopping the servers: " + e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	public void run ()
	{
		while(true)
			handleCommunication();
	}

	// Takes care of requests from replica leader multicast and sends replies to the replica leader UDP listener
	private void handleCommunication() 
	{
		try 
		{
			buffer = new byte [UDP_BUFFER_SIZE];
			requestFromLeaderPacket = new DatagramPacket(buffer, buffer.length);
			aMulticastSocket.receive(requestFromLeaderPacket);
			String resp = new String(requestFromLeaderPacket.getData());
			System.out.println("FROM RL -- " + resp);
			messageArray = resp.split(UDP_PARSER);
			requestFromLeaderPacket.setLength(buffer.length);
			
			for(int i =0; i<messageArray.length; i++) {
				messageArray[i] = messageArray[i].trim();
			}
			
			if(messageArray[0].equals(LR_NAME))
			{
				messageArray[1] = messageArray[1].trim();
				
				if(messageArray[1].equals(ACTION_TO_PERFORM.PLAYER_CREATE_ACCOUNT.name()))
				{
					messageArray[6] = messageArray[6].trim();
					setORBreference(messageArray[6]);
					String r_Result = aInterfaceIDL.createPlayerAccount(messageArray[2], messageArray[3], messageArray[4],
							 messageArray[5], messageArray[6], Integer.parseInt(messageArray[7].trim()));
					data = RB_NAME + UDP_PARSER + r_Result + UDP_PARSER + UDP_END_PARSE;
				}
				else if(messageArray[1].equals(ACTION_TO_PERFORM.PLAYER_SIGN_IN.name()))
				{
					messageArray[4] = messageArray[4].trim();
					setORBreference(messageArray[4]);
					String r_Result = aInterfaceIDL.playerSignIn(messageArray[2], messageArray[3], messageArray[4]);
					data = RB_NAME + UDP_PARSER + r_Result + UDP_PARSER + UDP_END_PARSE;
				}
				else if(messageArray[1].equals(ACTION_TO_PERFORM.PLAYER_SIGN_OUT.name()))
				{
					messageArray[3] = messageArray[3].trim();
					setORBreference(messageArray[3]);
					String r_Result = aInterfaceIDL.playerSignOut(messageArray[2], messageArray[3]);				
					data = RB_NAME + UDP_PARSER + r_Result + UDP_PARSER + UDP_END_PARSE;
				}
				else if(messageArray[1].equals(ACTION_TO_PERFORM.ADMIN_SIGN_IN.name()))
				{
					messageArray[4] = messageArray[4].trim();
					setORBreference(messageArray[4]);
					String r_Result = aInterfaceIDL.adminSignIn(messageArray[2], messageArray[3], messageArray[4]);
					data = RB_NAME + UDP_PARSER + r_Result + UDP_PARSER + UDP_END_PARSE;
				}
				else if(messageArray[1].equals(ACTION_TO_PERFORM.ADMIN_SIGN_OUT.name()))
				{
					messageArray[3] = messageArray[3].trim();
					setORBreference(messageArray[3]);
					String r_Result = aInterfaceIDL.adminSignOut(messageArray[2], messageArray[3]);
					data = RB_NAME + UDP_PARSER + r_Result + UDP_PARSER + UDP_END_PARSE;
				}
				else if(messageArray[1].equals(ACTION_TO_PERFORM.PLAYER_TRANSFER_ACCOUNT.name()))
				{
					messageArray[5] = messageArray[5].trim();
					setORBreference(messageArray[4]);
					String r_Result = aInterfaceIDL.transferAccount(messageArray[2], messageArray[3], messageArray[4], messageArray[5]);
					data = RB_NAME + UDP_PARSER + r_Result + UDP_PARSER + UDP_END_PARSE;
				}
				else if(messageArray[1].equals(ACTION_TO_PERFORM.ADMIN_SUSPEND_PLAYER_ACCOUNT.name()))
				{
					messageArray[5] = messageArray[5].trim();
					setORBreference(messageArray[4]);
					String r_Result = aInterfaceIDL.suspendAccount(messageArray[2], messageArray[3], messageArray[4], messageArray[5]);
					data = RB_NAME + UDP_PARSER + r_Result + UDP_PARSER + UDP_END_PARSE;
				}
				else if(messageArray[1].equals(ACTION_TO_PERFORM.ADMIN_GET_PLAYER_STATUS.name())){
					messageArray[4] = messageArray[4].trim();
					setORBreference(messageArray[4]);
					data = RB_NAME + UDP_PARSER + 
							aInterfaceIDL.getPlayerStatus(messageArray[2], messageArray[3], messageArray[4]) +
							UDP_PARSER + UDP_END_PARSE;
				}
				buffer = new byte [UDP_BUFFER_SIZE];
				buffer = data.getBytes();
				replyToLeaderPacket = new DatagramPacket(buffer, data.length(),  InetAddress.getByName("localhost"), UDP_PORT_REPLICA_LEAD);
				aSendSocket.send(replyToLeaderPacket);
				System.out.println("Sent back results to replica leader : " + data.toString());
				data = "";
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.out.println("UDP crashed, closing UDP Socket");
			aSendSocket.close();
			System.out.println("UDP crashed, creating new UDP Socket");
			try {
				aSendSocket = new DatagramSocket();
			} catch (SocketException e1) {
				System.out.println("UDP Socket creation failed");
			}
		}
	}
}
