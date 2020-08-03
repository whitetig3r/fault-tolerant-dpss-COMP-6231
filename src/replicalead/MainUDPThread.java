package replicalead;

import java.net.*;
import java.io.*;

import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;


public class MainUDPThread extends Thread {
	private static final int UDP_PORT_REPLICA_LEAD = 4000;
	private static final int UDP_PORT_REPLICA_LEAD_MULTICAST = 4446;
	private static final String UDP_ADDR_REPLICA_COMMUNICATION_MULTICAST = "224.0.0.2";
	
	private static final String UDP_PARSER = "/";
	private static final int UDP_BUFFER_SIZE = 25000;
	private static final String LR_NAME = "LR";
	
	private String m_UDPDataGram_from_stripped;
	
	@Override
	public void run() {		
		try {
			set_UDP_Server_Online();
		} 
		catch (Exception e) {
			System.out.println("Server thread interrupted.");
		}
	}
	
	MainUDPThread(){ }
	
	protected void set_UDP_Server_Online() throws InvalidName, ServantAlreadyActive, WrongPolicy, ObjectNotActive, AdapterInactive, InterruptedException {	
		DatagramSocket aSocket = null;
		try {
	    	aSocket = new DatagramSocket(UDP_PORT_REPLICA_LEAD);
			System.out.println("UDP_replicaLeader.setUDPServerOnline: UDP_replicaLeader going online.");
 			while(true){  
 				byte[] buffer = new byte[UDP_BUFFER_SIZE];
 				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
  				aSocket.receive(request);     
  				String l_result = new String(request.getData(), "UTF-8");
  				
  				if(l_result != null)
  				{
  					System.out.println(l_result);
  					String l_senderName = parseSenderName(l_result);
  					if(l_senderName != null)
  					{
  						switch(l_senderName) 
  						{
  				    		case "FE":
  				    				System.out.println("Receiving data from FE.");
  				    				RequestProcessor l_LocalOrbProcessing = new RequestProcessor();
  				    				ReplicaRequestProcessor.m_HasBeenProcessed = false;
  				    				if(m_UDPDataGram_from_stripped != "")
  				    				{
  				    				
  	  				    				String l_multiCastDGram_replica =  LR_NAME + UDP_PARSER + m_UDPDataGram_from_stripped;
  				    					System.out.println("UDP_replicaLeader.set_UDP_Server_Online : l_multiCastDGram_replica - "+ l_multiCastDGram_replica);
  				    					ReplicaRequestProcessor.m_LeaderResultProcessed = l_LocalOrbProcessing.performRMI(m_UDPDataGram_from_stripped);
  	  				    				sendMulticastPacket_Replicas(l_multiCastDGram_replica);
  				    					
  				    				}
  				    				
  				    				l_LocalOrbProcessing = null;
  				    				m_UDPDataGram_from_stripped = "";  				    				
  				    				break;
  				    
  				    		case "RM":
  				    				System.out.println("Receiving data from RM: m_UDPDataGram_from_stripped - " + m_UDPDataGram_from_stripped);
  				    				RequestProcessor l_LocalRMRequestProcessing = new RequestProcessor();
  				    				l_LocalRMRequestProcessing.ProcessRMRequests(m_UDPDataGram_from_stripped);
  				    				m_UDPDataGram_from_stripped = "";
  				    			break;
  				    		
  				    		case "RA":
  				    				// result of a certain request
  				    				System.out.println("Receiving data from RA: m_UDPDataGram_from_stripped - " + m_UDPDataGram_from_stripped);
  				    				
  				    				if(m_UDPDataGram_from_stripped != "") {
  				    					ReplicaRequestProcessor.m_Replica_A_Processed = m_UDPDataGram_from_stripped;
  				    					ReplicaRequestProcessor.CompareResults();
  				    				}
  				    				
  				    				m_UDPDataGram_from_stripped = "";
  				    				
  				    			break;
  				    		
  				    		case "RB":
  				    				// result of a certain request
  				    			System.out.println("Receiving data from RB: m_UDPDataGram_from_stripped - " + m_UDPDataGram_from_stripped);
  				    			if(m_UDPDataGram_from_stripped != ""){
				    					ReplicaRequestProcessor.m_Replica_B_Processed = m_UDPDataGram_from_stripped;
				    					ReplicaRequestProcessor.CompareResults();
				    				}
				    				
				    				m_UDPDataGram_from_stripped = "";
  				    			break;
  				    		
  				    		default:
  				    				System.out.println("Unknown Sender. Protocol not being followed");
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
	
	public static boolean sendPacket(String p_Data, int p_portNumber) {
		DatagramSocket aSocket = null;
		try 
		{
			aSocket = new DatagramSocket();    
			byte [] m = p_Data.getBytes();
			InetAddress aHost = InetAddress.getByName("localhost");
			int serverPort = p_portNumber;		                                                 
			DatagramPacket request = new DatagramPacket(m,  p_Data.length(), aHost, serverPort);
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
	
	protected boolean sendMulticastPacket_Replicas(String p_Data) throws IOException, InterruptedException
	{
		DatagramSocket socket = null; 

		try 
		{
			socket = new DatagramSocket();

			byte[] buffer = p_Data.getBytes();
			DatagramPacket dgram;
			
			System.out.println("UDP_replicaLeader.sendMulticastPacket_Replicas : p_Data - "+ p_Data);
			
			dgram = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(UDP_ADDR_REPLICA_COMMUNICATION_MULTICAST), UDP_PORT_REPLICA_LEAD_MULTICAST);
			//while(true) 
			{
				//System.err.print(".");
				socket.send(dgram);
				//Thread.sleep(1000);
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
			if(socket != null) socket.close();
		}
		
		return false;
	}
	
	
	
	// Parse the datagram and extract the senders name into a String Array
	protected String parseSenderName(String p_input) {
		String l_segments[] = p_input.split(UDP_PARSER);
		if(l_segments != null)
		{
			m_UDPDataGram_from_stripped = p_input.substring(3, p_input.length());
			//System.out.println("UDP_replicaLeader.parseSenderName: m_UDPDataGram_from_stripped - " + m_UDPDataGram_from_stripped);
			return l_segments[0];
		}
		System.out.println("UDP_replicaLeader.parseSenderName: failed to parse udp packet data");
		return null;
	}
	
		
	public static void main(String[] args)  
	{
		MainUDPThread m_UDP_replicaLeader = new MainUDPThread();
		m_UDP_replicaLeader.start();
	}
}
