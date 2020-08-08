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

import exceptions.UnknownServerRegionException;

class ORBThread extends Thread
{
	private ORB orb;

	protected ORBThread(ORB orb)
	{
		this.orb = orb;
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
	private boolean listenerCrashed;
	private boolean listenerRestartNeeded;
	private DatagramSocket aDatagramSocket;
	private DatagramPacket requestFromReplicaManager;
	private GameServer gameServerReference;
	private byte [] buffer;
	private String [] parameterList;
	
	private final String MSG_SEP = "/";
	private int UDP_BUFFER_SIZE = 1200;
	protected static String BREAKER_IDENTIFIER = "BRE";
	private static final String REPLICA_MANAGER_IDENTIFIER = "RM";
	protected static int REPLICA_BREAKER_PORT = 9393;

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
	
	protected ReplicaManagerListenUDPThread(int port) throws SocketException
	{
		aPort = port;
		listenerCrashed = false;
		listenerRestartNeeded = false;
		aDatagramSocket = new DatagramSocket(aPort);
		buffer = new byte [UDP_BUFFER_SIZE];
	}
	
	protected boolean hasCrashed()
	{
		return listenerCrashed;
	}
	
	protected boolean shouldRestart()
	{
		return listenerRestartNeeded;
	}
	
	protected void resetShouldRestart()
	{
		listenerRestartNeeded = false;
	}
	
	@Override
	public void run ()
	{
		while(true)
			handleCommunication();
	}
	
	public void handleCommunication()
	{
		try
		{
			requestFromReplicaManager = new DatagramPacket(buffer, buffer.length);
			aDatagramSocket.receive(requestFromReplicaManager);
			parameterList = (new String(requestFromReplicaManager.getData())).split(MSG_SEP);
			if(parameterList[0].equals(REPLICA_MANAGER_IDENTIFIER))
			{
				parameterList[1] = parameterList[1].trim();
				if(parameterList[1].equals(ACTION_TO_PERFORM.RESTART_REPLICA.name()))
				{
					listenerRestartNeeded = true;
				}
			} else if(parameterList[0].equals(BREAKER_IDENTIFIER)) {
				setORBreference("132.168.2.22");
				boolean na = gameServerReference.initiateCorruption();
				setORBreference("93.168.2.22");
				boolean eu = gameServerReference.initiateCorruption();
				setORBreference("182.168.2.22");
				boolean as = gameServerReference.initiateCorruption();
				if(na && eu && as && sendCorruptionSuccessPacket("success"))
					System.out.println("Sent confirmation to breaker server");
					
			}
		}
		catch (IOException e)
		{
			aDatagramSocket.close();
			listenerCrashed = true;
		}
	}
	
	
	private boolean setORBreference(String ipAddress) throws IOException
	{
		ORB orb = ORB.init(new String [1], null);
		BufferedReader bufferedReader;
		if(ipAddress.length() >= 3 && ipAddress.substring(0,3).equals("132"))
		{
			bufferedReader = new BufferedReader(new FileReader("NA_BIOR.txt"));
		}
		else if(ipAddress.length() >= 2 && ipAddress.substring(0,2).equals("93"))
		{
			bufferedReader = new BufferedReader(new FileReader("EU_BIOR.txt"));
		}
		else if(ipAddress.length() >= 3 && ipAddress.substring(0,3).equals("182"))
		{
			bufferedReader = new BufferedReader(new FileReader("AS_BIOR.txt"));
		}
		else
		{
			System.out.println("ERR: Unknown Region");
			return false;
		}
		String stringORB = bufferedReader.readLine();
		bufferedReader.close();
		org.omg.CORBA.Object reference_CORBA = orb.string_to_object(stringORB);
		gameServerReference = GameServerHelper.narrow(reference_CORBA);
		
		orb = null;
		stringORB = null;
		bufferedReader = null;
		
		return true;
	}

	private boolean sendCorruptionSuccessPacket(String requestData) {
			DatagramSocket aSocket = null;
			try 
			{
				aSocket = new DatagramSocket();    
				byte [] m = requestData.getBytes();
				InetAddress aHost = InetAddress.getByName("localhost");
				int serverPort = REPLICA_BREAKER_PORT;		                                                 
				DatagramPacket request = new DatagramPacket(m,  requestData.length(), aHost, serverPort);
				aSocket.send(request);		
				return true;
			}
			catch (SocketException e)
			{
				System.out.println("Socket: " + e.getMessage());
			}
			catch (IOException e)
			{
				System.out.println("IO: " + e.getMessage());
			}
			finally 
			{
				if(aSocket != null) aSocket.close();
			}
			
			return false;
	}
}

class MainUDPThread extends Thread
{
	
	private final String MSG_SEP = "/";
	private int UDP_BUFFER_SIZE = 1200;
	private static MainUDPThread replicaTwo;
	private GameServer gameServerReference;
	private GameServerServant NAGameServer;
	private GameServerServant EUGameServer;
	private GameServerServant ASGameServer;
	private Thread NAServerThread;
	private Thread EUServerThread;
	private Thread ASServerThread;
	private static MulticastSocket aMulticastSocket;
	private DatagramPacket requestFromLeaderPacket;
	private byte [] buffer;
	private String [] parameterList;
	private DatagramSocket aSendSocket;
	private DatagramPacket replyToLeaderPacket;
	private String data;
	protected ReplicaManagerListenUDPThread replicaManagerListener;
	
	protected static int REPLICA_TWO_PORT = 3000;
	
	protected static String REPLICA_MANAGER_IDENTIFIER = "RM";
	protected static String REPLICA_LEADER_IDENTIFIER = "LR";
	protected static String REPLICA_TWO_IDENTIFIER = "RB";
	protected static String UDP_END_PARSE = "$";
	protected static String R2_NA_NAME = "NA";
	protected static String R2_EU_NAME = "EU";
	protected static String R2_AS_NAME = "AS";
	protected static int REPLICA_LEAD_PORT = 4000;
	
	protected static int REPLICA_LEAD_MULTICAST_PORT = 4446;
	
	protected static String MULTICAST_IP_ADDR = "224.0.0.2";
	
	protected static String PREFIX_NA = "132";
	protected static String PREFIX_EU = "93";
	protected static String PREFIX_AS = "182";
	
	public static void main(String[] args) throws InterruptedException 
	{
		String[] defaultArgs = {};
		replicaTwo = new MainUDPThread(REPLICA_TWO_PORT, defaultArgs);
		while (true)
		{
			if(replicaTwo.replicaManagerListener.shouldRestart())
			{
				System.out.println("Restarting Replica Group Two...");
				replicaTwo.stopReplicaGroup();
				replicaTwo.startReplicaGroup();
				replicaTwo.replicaManagerListener.resetShouldRestart();
			}
			if(replicaTwo.replicaManagerListener.hasCrashed())
			{
				System.out.println("ERR: Crashed. Restarting UDP listener...");
				try {
					replicaTwo.replicaManagerListener = new ReplicaManagerListenUDPThread(REPLICA_TWO_PORT);
				} catch (SocketException e) {
					System.out.println("ERR: Failed to create Replica Manager listener");
				}
				replicaTwo.replicaManagerListener.start();
			}
			
			Thread.sleep(200);
		}
	}
	
	private MainUDPThread(int port, String[] args)
	{
		buffer = new byte [UDP_BUFFER_SIZE];
		try {
			aMulticastSocket = new MulticastSocket(REPLICA_LEAD_MULTICAST_PORT);
			aMulticastSocket.joinGroup(InetAddress.getByName(MULTICAST_IP_ADDR));
			aSendSocket = new DatagramSocket();
			replicaManagerListener = new ReplicaManagerListenUDPThread(REPLICA_TWO_PORT);
			System.out.println("Starting UDP Listener...");
			replicaManagerListener.start();
			start();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("ERR: Failed to create UDP Socket...");
		}
	}
	
	private boolean setORBreference(String ipAddress) throws IOException
	{
		ORB orb = ORB.init(new String [1], null);
		BufferedReader bufferedReader;
		if(ipAddress.length() >= 3 && ipAddress.substring(0,3).equals(PREFIX_NA))
		{
			bufferedReader = new BufferedReader(new FileReader(R2_NA_NAME + "_BIOR.txt"));
		}
		else if(ipAddress.length() >= 2 && ipAddress.substring(0,2).equals(PREFIX_EU))
		{
			bufferedReader = new BufferedReader(new FileReader(R2_EU_NAME + "_BIOR.txt"));
		}
		else if(ipAddress.length() >= 3 && ipAddress.substring(0,3).equals(PREFIX_AS))
		{
			bufferedReader = new BufferedReader(new FileReader(R2_AS_NAME + "_BIOR.txt"));
		}
		else
		{
			System.out.println("Unknown Region");
			return false;
		}
		String stringORB = bufferedReader.readLine();
		bufferedReader.close();
		org.omg.CORBA.Object reference_CORBA = orb.string_to_object(stringORB);
		gameServerReference = GameServerHelper.narrow(reference_CORBA);
		
		orb = null;
		stringORB = null;
		bufferedReader = null;
		
		return true;
	}
	
	protected boolean startReplicaGroup()
	{
		try
		{
			createRegionServerInstances();
			spawnRegionServerThreads();
			System.out.println("All region servers restarted...");
		}
		catch(Exception e)
		{
			System.out.println("ERR: Failed to restart region servers " + e.getMessage());
			return false;
		}
		return true;
	}

	private void spawnRegionServerThreads() {
		NAServerThread = new Thread(NAGameServer);
		EUServerThread = new Thread(EUGameServer);
		ASServerThread = new Thread(ASGameServer);
		NAServerThread.start();
		EUServerThread.start();
		ASServerThread.start();
	}

	private void createRegionServerInstances() throws UnknownServerRegionException {
		NAGameServer = new GameServerServant(R2_NA_NAME, new ArrayList<>(Arrays.asList(9990,9991)),9989);
		EUGameServer = new GameServerServant(R2_EU_NAME, new ArrayList<>(Arrays.asList(9989,9991)),9990);
		ASGameServer = new GameServerServant(R2_AS_NAME, new ArrayList<>(Arrays.asList(9989,9990)),9991);
	}
	
	protected boolean stopReplicaGroup()
	{
		try
		{
			NAServerThread = null;
			EUServerThread = null;
			ASServerThread = null;
			if(NAGameServer != null)
				NAGameServer.destroy();
			if(EUGameServer != null)
				EUGameServer.destroy();
			if(ASGameServer != null)
				ASGameServer.destroy();
			NAGameServer = null;
			EUGameServer = null;
			ASGameServer = null;
			System.out.println("Halting all region servers...");
		}
		catch(Exception e)
		{
			System.out.println("ERR: Failed to halt region servers " + e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	public void run ()
	{
		while(true)
			performAction();
	}

	private void performAction() 
	{
		try 
		{
			buffer = new byte [UDP_BUFFER_SIZE];
			requestFromLeaderPacket = new DatagramPacket(buffer, buffer.length);
			aMulticastSocket.receive(requestFromLeaderPacket);
			String resp = new String(requestFromLeaderPacket.getData());
			parameterList = resp.split(MSG_SEP);
			requestFromLeaderPacket.setLength(buffer.length);
			
			sanitizeRequest();
			
			if(parameterList[0].equals(REPLICA_LEADER_IDENTIFIER))
			{
				performRequestedAction();
				buffer = new byte [UDP_BUFFER_SIZE];
				buffer = data.getBytes();
				replyToLeaderPacket = new DatagramPacket(buffer, data.length(),  InetAddress.getByName("localhost"), REPLICA_LEAD_PORT);
				aSendSocket.send(replyToLeaderPacket);
				System.out.println("Dispatching response to Replica Leader..." + data.toString());
				data = "";
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.out.println("ERR: Crash detected. UDP Socket is closed. Attempting to create new UDP Socket...");
			aSendSocket.close();
			try {
				aSendSocket = new DatagramSocket();
			} catch (SocketException e1) {
				System.out.println("ERR: Failed to create UDP Socket");
			}
		}
	}

	private void sanitizeRequest() {
		for(int i =0; i<parameterList.length; i++) {
			parameterList[i] = parameterList[i].trim();
		}
	}

	private void performRequestedAction() throws IOException {
		switch(parameterList[1]) {
			case "PLAYER_CREATE_ACCOUNT": {
				setORBreference(parameterList[6]);
				String orbResponse = gameServerReference.createPlayerAccount(parameterList[2], parameterList[3], parameterList[4],
						 parameterList[5], parameterList[6], Integer.parseInt(parameterList[7]));
				data = packageResponseForRL(orbResponse);
				break;
			}
			case "PLAYER_SIGN_IN": {
				setORBreference(parameterList[4]);
				String orbResponse = gameServerReference.playerSignIn(parameterList[2], parameterList[3], parameterList[4]);
				data = packageResponseForRL(orbResponse);
				break;
			}
			case "PLAYER_SIGN_OUT": {
				setORBreference(parameterList[3]);
				String orbResponse = gameServerReference.playerSignOut(parameterList[2], parameterList[3]);				
				data = packageResponseForRL(orbResponse);
				break;
			}
			case "ADMIN_SIGN_IN": {
				setORBreference(parameterList[4]);
				String orbResponse = gameServerReference.adminSignIn(parameterList[2], parameterList[3], parameterList[4]);
				data = packageResponseForRL(orbResponse);
				break;
			}
			case "ADMIN_SIGN_OUT": {
				setORBreference(parameterList[3]);
				String orbResponse = gameServerReference.adminSignOut(parameterList[2], parameterList[3]);
				data = packageResponseForRL(orbResponse);
			}
			case "PLAYER_TRANSFER_ACCOUNT": {
				setORBreference(parameterList[4]);
				String orbResponse = gameServerReference.transferAccount(parameterList[2], parameterList[3], parameterList[4], parameterList[5]);
				data = packageResponseForRL(orbResponse);
				break;
			}
			case "ADMIN_SUSPEND_PLAYER_ACCOUNT": {
				setORBreference(parameterList[4]);
				String orbResponse = gameServerReference.suspendAccount(parameterList[2], parameterList[3], parameterList[4], parameterList[5]);
				data = packageResponseForRL(orbResponse);
				break;
			}
			case "ADMIN_GET_PLAYER_STATUS": {
				setORBreference(parameterList[4]);
				data = packageResponseForRL(gameServerReference.getPlayerStatus(parameterList[2], parameterList[3], parameterList[4]));
				break;
			}
		}
	}
	
	private String packageResponseForRL(String responseData) {
		return REPLICA_TWO_IDENTIFIER + MSG_SEP + responseData + MSG_SEP + UDP_END_PARSE;
	}
}
