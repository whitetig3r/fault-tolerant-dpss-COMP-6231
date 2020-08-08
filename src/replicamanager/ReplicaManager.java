package replicamanager;

import java.io.*;
import java.net.*;

public class ReplicaManager 
{
	static int ipAddress = 0;
	static int port;
	static String requestData = null;
	static String [] parameterList;
	private static DatagramSocket aSocket = null;
	private static boolean holdConn = true;
	static int parserPosition = 0;
	private static final int REPLICA_ONE_PORT = 2000;
	private static final int REPLICA_TWO_PORT = 3000;
	private static final int REPLICA_LEAD_PORT = 4000;
	private static final int REPLICA_MANAGER_PORT = 5000;
	private static int UDP_BUFFER_SIZE = 1200;
	private final static String REPLICA_MANAGER_IDENTIFIER = "RM"; 
	private final static String REPLICA_ONE_IDENTIFIER = "RA";
	private final static String REPLICA_TWO_IDENTIFIER = "RB";
	private final static String REPLICA_LEAD_IDENTIFIER = "LR";
	private static int replicaOnecounter = 0;
	private static int replicaTwoCounter = 0;
	private static enum ACTION_TO_PERFORM {
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
	
	private ReplicaManager()
	{
		startAllReplicaGroups();
		startReplicaManagerListener(REPLICA_MANAGER_PORT);
	}

	public static void main (String [] args) 
	{
		new ReplicaManager();
	}

	protected static void startReplicaManagerListener (int portNumber) {
		try {

			aSocket = new DatagramSocket(REPLICA_MANAGER_PORT);
			byte [] buffer = new byte [UDP_BUFFER_SIZE];
			while (holdConn) {							
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(request);
				requestData = new String(request.getData());
				parameterList = requestData.split("/");
				if(parameterList[0].equals(REPLICA_LEAD_IDENTIFIER))
				{
					handleInconsistentReplicaGroup();
				}
			}
		}
			catch (Exception e) {e.printStackTrace();} 
	}

	private static void handleInconsistentReplicaGroup() {
		if (parameterList[1].contains(REPLICA_ONE_IDENTIFIER)) 
		{
			replicaOnecounter ++;
			if(replicaOnecounter >= 3) 
			{
				replicaOnecounter = 0;
				stopReplicaGroup(REPLICA_ONE_PORT);
			}
		}
		else if (parameterList[1].contains(REPLICA_TWO_IDENTIFIER)) 
		{
			replicaTwoCounter ++;
			if(replicaTwoCounter >= 3) 
			{
				replicaTwoCounter = 0;
				stopReplicaGroup(REPLICA_TWO_PORT);
			}
		}
	}
	
	private void startAllReplicaGroups() {
		startReplicaGroup(REPLICA_LEAD_PORT);
		startReplicaGroup(REPLICA_ONE_PORT);
		startReplicaGroup(REPLICA_TWO_PORT);
		System.out.println ("Replica Manager has requested RESTART of all server groups...");
	}

	protected static void startReplicaGroup(int portNumber){
		int UDPcommunicationPort = portNumber;
		DatagramSocket aSocket = null;
		String requestReplicaManagerMessage = REPLICA_MANAGER_IDENTIFIER + "/" + ACTION_TO_PERFORM.RESTART_REPLICA.name();

		try {
			aSocket = new DatagramSocket();
			byte [] m = requestReplicaManagerMessage.getBytes();
			InetAddress aHost = InetAddress.getByName("localhost");
			DatagramPacket request = new DatagramPacket(m,m.length, aHost, UDPcommunicationPort);
			aSocket.send(request);

		}
		catch (SocketException e){
			System.out.println("Socket " + e.getMessage());
		}
		catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		}

		finally {
			if (aSocket != null) {
				aSocket.close();
			}
		}
	}



	protected static boolean stopReplicaGroup (int portNumber) {
		int replicaGroupPort = portNumber;
		DatagramSocket aSocket = null;
		boolean response = true;
		String requestReplicaManagerMessage = REPLICA_MANAGER_IDENTIFIER + "/" + ACTION_TO_PERFORM.RESTART_REPLICA.name();

		try {
			aSocket = new DatagramSocket();
			byte [] m = requestReplicaManagerMessage.getBytes();
			InetAddress aHost = InetAddress.getByName("localhost");
			DatagramPacket request = new DatagramPacket(m,requestReplicaManagerMessage.length(), aHost, replicaGroupPort);
			aSocket.send(request);

		}
		catch (SocketException e){
			System.out.println("Socket " + e.getMessage());
			response = false;
		}
		catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
			response = false;
		}

		finally {
			if (aSocket != null) {
				aSocket.close();
			}
		}
		return response;

	}
}