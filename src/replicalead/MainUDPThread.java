package replicalead;

import java.net.*;
import java.io.*;

import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;


public class MainUDPThread extends Thread {
	private static final int REPLICA_LEAD_PORT = 4000;
	private static final int REPLICA_LEAD_MULTICAST_PORT = 4446;
	private static final String REPLICA_COMMUNICATION_MULTICAST_ADDR = "224.0.0.2";
	
	private static final String MSG_SEP = "/";
	private static final int BUFFER_SIZE = 1200;
	private static final String NAME_REPLICA_LEAD = "LR";
	
	private String extractedDatagram;
	
	@Override
	public void run() {		
		try {
			startUdpServer();
		} 
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("Server thread interrupted.");
		}
	}
	
	MainUDPThread(){ }
	
	protected void startUdpServer() throws InvalidName, ServantAlreadyActive, WrongPolicy, ObjectNotActive, AdapterInactive, InterruptedException {	
		DatagramSocket aSocket = null;
		try {
	    	aSocket = new DatagramSocket(REPLICA_LEAD_PORT);
			System.out.println("REPLICA LEADER -- UDP Thread is going online!");
 			while(true){  
 				byte[] buffer = new byte[BUFFER_SIZE];
 				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
  				aSocket.receive(request);     
  				String requestData = new String(request.getData(), "UTF-8");
  				
  				if(requestData != null)
  				{
  					String requestSender = extractSender(requestData);
  					if(requestSender != null)
  					{
  						switch(requestSender) 
  						{
  				    		case "FE":
  				    				System.out.println("Receiving Datagram from Front End...");
  				    				RequestProcessor requestProcessorFE = new RequestProcessor();
  				    				ReplicaRequestProcessor.requestProcessed = false;
  				    				if(extractedDatagram != "")
  				    				{
  				    				
  	  				    				String multicastDatagramData =  NAME_REPLICA_LEAD + MSG_SEP + extractedDatagram;
  				    					System.out.println("Datagram Data sent to Front End - " + multicastDatagramData);
  				    					ReplicaRequestProcessor.leaderResponse = requestProcessorFE.performORBAction(extractedDatagram);
  	  				    				sendMulticastToReplicaGroups(multicastDatagramData);
  				    					
  				    				}	    	
  				    				
  				    				requestProcessorFE = null;
  				    				extractedDatagram = "";  				    				
  				    				break;
  				    				
  				    		case "RM":
  				    				System.out.println("Receiving Datagram from Replica Manager... - " + extractedDatagram);
  				    				RequestProcessor requestProcessorRM = new RequestProcessor();
  				    				requestProcessorRM.ProcessRMRequests(extractedDatagram);
  				    				extractedDatagram = "";
  				    			break;
  				    		
  				    		case "RA":
  				    				// result of a certain request
  				    				System.out.println("Receiving Datagram from Replica 1... - " + extractedDatagram);
  				    				
  				    				if(extractedDatagram != "") {
  				    					ReplicaRequestProcessor.replicaOneResponse = extractedDatagram;
  				    					ReplicaRequestProcessor.verifyConsistentResults();
  				    				}
  				    				
  				    				extractedDatagram = "";
  				    			break;
  				    		
  				    		case "RB":
  				    				// result of a certain request
  				    			System.out.println("Receiving Datagram from Replica 2... - " + extractedDatagram);
  				    			if(extractedDatagram != ""){
				    					ReplicaRequestProcessor.replicaTwoResponse = extractedDatagram;
				    					ReplicaRequestProcessor.verifyConsistentResults();
				    				}
				    				
				    				extractedDatagram = "";
  				    			break;
  				    			
  				    		default:
  				    				System.out.println("ERR: Sender is UNKNOWN.");
  				    				break;
  						}	
  					}
  				}
    		}
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
    }
	
	public static boolean sendPacket(String requestData, int port) {
		DatagramSocket aSocket = null;
		try 
		{
			aSocket = new DatagramSocket();    
			byte [] m = requestData.getBytes();
			InetAddress aHost = InetAddress.getByName("localhost");
			int serverPort = port;		                                                 
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
	
	protected boolean sendMulticastToReplicaGroups(String requestData) throws IOException, InterruptedException
	{
		DatagramSocket socket = null; 

		try 
		{
			socket = new DatagramSocket();

			byte[] buffer = requestData.getBytes();
			DatagramPacket dataGram;
			
			System.out.println("Dispatching multicast request to replica groups -- "+ requestData);
			
			dataGram = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(REPLICA_COMMUNICATION_MULTICAST_ADDR), REPLICA_LEAD_MULTICAST_PORT);
			socket.send(dataGram);
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
			if(socket != null) socket.close();
		}
		
		return false;
	}
	
	// Parse the datagram and extract the senders name into a String Array
	protected String extractSender(String responseData) {
		String extractedParts[] = responseData.split(MSG_SEP);
		if(extractedParts != null)
		{
			extractedDatagram = responseData.substring(3, responseData.length());
			return extractedParts[0];
		}
		System.out.println("ERR: Failed to extract sender information");
		return null;
	}
	
		
	public static void main(String[] args)  
	{
		MainUDPThread replicaLeaderThread = new MainUDPThread();
		replicaLeaderThread.start();
	}
}
