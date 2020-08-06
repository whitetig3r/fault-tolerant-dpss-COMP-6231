package replicamanager;

import java.io.*;
import java.net.*;

public class ReplicaManager 
{

	private static int replicaOnecounter = 0 , replicaTwoCounter = 0;

	private static DatagramSocket aSocket = null;
	private static boolean waitForConnection = true;
	static int IPaddress = 0;
	static int serverPort;
	static String dataRecieved = null;
	static String [] messageArray;
	static int parserPosition = 0;
	
	private static final int UDP_PORT_REPLICA_ONE = 2000;
	private static final int UDP_PORT_REPLICA_TWO = 3000;
	private static final int UDP_PORT_REPLICA_LEAD = 4000;
	private static final int UDP_PORT_REPLICA_MANAGER = 5000;
	private static int UDP_BUFFER_SIZE = 1200;
	private final static String RM_NAME = "RM"; 
	private final static String RA_NAME = "RA";
	private final static String RB_NAME = "RB";
	private final static String LR_NAME = "LR";
	
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
		//Initialize the system by sending 3 UDP messages to 3 server groups
		startServerGroup(UDP_PORT_REPLICA_LEAD);
		startServerGroup(UDP_PORT_REPLICA_ONE);
		startServerGroup(UDP_PORT_REPLICA_TWO);

		System.out.println ("Replica Manager sent request to run all servers!");
		startServerListener(UDP_PORT_REPLICA_MANAGER);
	}

	public static void main (String [] args) 
	{
		new ReplicaManager();
	}

	protected static void startServerListener (int portNumber) {
		String requestServerInitials = null;

		try {

			aSocket = new DatagramSocket(UDP_PORT_REPLICA_MANAGER);
			byte [] buffer = new byte [UDP_BUFFER_SIZE];

			//always listen for new messages on the specified port
			while (waitForConnection) {							

				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(request);

				//get the data from the request and check
				dataRecieved = new String(request.getData());
				//dataRecieved.toUpperCase();
				messageArray = dataRecieved.split("/");

				//parserPosition = dataRecieved.indexOf(Parameters.UDP_PARSER);
				System.out.println(dataRecieved);

				if(messageArray[0].equals(LR_NAME))
				{
					//check the message from the Replica A
					if (messageArray[1].contains(RA_NAME)) 
					{
						replicaOnecounter ++;
						if(replicaOnecounter >= 3) 
						{
							replicaOnecounter = 0;
							stopServer(UDP_PORT_REPLICA_ONE);
						}
					} //check the message from the Replica B
					else if (messageArray[1].contains(RB_NAME)) 
					{
						replicaTwoCounter ++;
						if(replicaTwoCounter >= 3) 
						{
							replicaTwoCounter = 0;
							stopServer(UDP_PORT_REPLICA_TWO);
						}
					}
				}
			}
		}
			catch (Exception e) {e.printStackTrace();} 
		}

	protected static void startServerGroup(int portNumber){
		int UDPcommunicationPort = portNumber;
		DatagramSocket aSocket = null;
		String RMInitMessage = RM_NAME + "/" + ACTION_TO_PERFORM.RESTART_REPLICA.name();
		boolean ackRecieved = true;

		try {
			aSocket = new DatagramSocket();
			byte [] m = RMInitMessage.getBytes();
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



	protected static boolean stopServer (int portNumber) {
		int stopServerPort = portNumber;
		DatagramSocket aSocket = null;
		String RMInitMessage = RM_NAME + "/" + ACTION_TO_PERFORM.RESTART_REPLICA.name();
		boolean ackRecieved = true;

		try {
			aSocket = new DatagramSocket();
			byte [] m = RMInitMessage.getBytes();
			InetAddress aHost = InetAddress.getByName("localhost");
			DatagramPacket request = new DatagramPacket(m,RMInitMessage.length(), aHost, stopServerPort);
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