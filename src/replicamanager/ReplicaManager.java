package replicamanager;

import java.io.*;
import java.net.*;

public class ReplicaManager 
{

	private static int replicaOnecounter = 0 , replicaTwoCounter = 0;

	private static DatagramSocket aSocket = null;
	private static boolean waitForConnection = true;
	static int ipAddress = 0;
	static int portServer;
	static String dataRecieved = null;
	static String [] messageArray;
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
	
	// private constructor
	private ReplicaManager()
	{
		startReplicaGroup(REPLICA_LEAD_PORT);
		startReplicaGroup(REPLICA_ONE_PORT);
		startReplicaGroup(REPLICA_TWO_PORT);

		System.out.println ("Replica Manager has requested RESTART of all server groups...");
		startReplicaManagerListener(REPLICA_MANAGER_PORT);
	}

	public static void main (String [] args) 
	{
		new ReplicaManager();
	}

	protected static void startReplicaManagerListener (int portNumber) {
		String requestServerInitials = null;

		try {

			aSocket = new DatagramSocket(REPLICA_MANAGER_PORT);
			byte [] buffer = new byte [UDP_BUFFER_SIZE];
			
			while (waitForConnection) {							

				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(request);
				dataRecieved = new String(request.getData());
				messageArray = dataRecieved.split("/");
				if(messageArray[0].equals(REPLICA_LEAD_IDENTIFIER))
				{
					if (messageArray[1].contains(REPLICA_ONE_IDENTIFIER)) 
					{
						replicaOnecounter ++;
						if(replicaOnecounter >= 3) 
						{
							replicaOnecounter = 0;
							stopReplicaGroup(REPLICA_ONE_PORT);
						}
					}
					else if (messageArray[1].contains(REPLICA_TWO_IDENTIFIER)) 
					{
						replicaTwoCounter ++;
						if(replicaTwoCounter >= 3) 
						{
							replicaTwoCounter = 0;
							stopReplicaGroup(REPLICA_TWO_PORT);
						}
					}
				}
			}
		}
			catch (Exception e) {e.printStackTrace();} 
	}

	protected static void startReplicaGroup(int portNumber){
		int UDPcommunicationPort = portNumber;
		DatagramSocket aSocket = null;
		String requestReplicaManagerMessage = REPLICA_MANAGER_IDENTIFIER + "/" + ACTION_TO_PERFORM.RESTART_REPLICA.name();
		boolean ackRecieved = true;

		try {
			aSocket = new DatagramSocket();
			byte [] m = requestReplicaManagerMessage.getBytes();
			InetAddress aHost = InetAddress.getByName("localhost");
			DatagramPacket request = new DatagramPacket(m,m.length, aHost, UDPcommunicationPort);
			aSocket.send(request);

		}
		catch (SocketException e){
			System.out.println("Socket " + e.getMessage());
			ackRecieved = false;
		}
		catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
			ackRecieved = false;
		}

		finally {
			if (aSocket != null) {
				aSocket.close();
			}
		}
	}



	protected static boolean stopReplicaGroup (int portNumber) {
		int stopServerPort = portNumber;
		DatagramSocket aSocket = null;
		String requestReplicaManagerMessage = REPLICA_MANAGER_IDENTIFIER + "/" + ACTION_TO_PERFORM.RESTART_REPLICA.name();
		boolean ackRecieved = true;

		try {
			aSocket = new DatagramSocket();
			byte [] m = requestReplicaManagerMessage.getBytes();
			InetAddress aHost = InetAddress.getByName("localhost");
			DatagramPacket request = new DatagramPacket(m,requestReplicaManagerMessage.length(), aHost, stopServerPort);
			aSocket.send(request);

		}
		catch (SocketException e){
			System.out.println("Socket " + e.getMessage());
			ackRecieved = false;
		}
		catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
			ackRecieved = false;
		}

		finally {
			if (aSocket != null) {
				aSocket.close();
			}
		}
		return ackRecieved;

	}
}