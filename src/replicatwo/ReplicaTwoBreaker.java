package replicatwo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReplicaTwoBreaker {
	private static final int UDP_BREAKER_PORT = 9393;

	public static void main(String[] args) {
		runRegionUdpServer();
		makeInvalidResultsOnReplica();
	}

	private static void makeInvalidResultsOnReplica() {
		DatagramSocket aSocket = null;
		String reqOp = "BRE/";
		int UDP_REPLICA_TWO = 3000;
		try {
			aSocket = new DatagramSocket();    
			aSocket.setSoTimeout(5000); 
			byte [] m = reqOp.getBytes();
			InetAddress aHost = InetAddress.getByName("127.0.0.1");		                                                 
			DatagramPacket request =
			 	new DatagramPacket(m, reqOp.length(), aHost, UDP_REPLICA_TWO);
			aSocket.send(request);			                        
			System.out.println("Successfully initiated replicaTwo server corruption on all three region servers!");
		} catch (SocketTimeoutException e) {
			String timeOut = String.format("ERR: Request to server on port %d has timed out!", UDP_REPLICA_TWO);
			System.out.println(timeOut);
		} catch (SocketException e){
			System.out.println("ERR: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("ERR: " + e.getMessage());
		} finally {
			if(aSocket != null) aSocket.close();
		}
	}
	
	private static void runRegionUdpServer() {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		
    	executorService.execute((Runnable) ()->{
    	  String log = String.format("Starting UDP Server for breaker to receive confirmation on port %d...", UDP_BREAKER_PORT); 
		  System.out.println(log);
		  listenForServerRequests();
		});
		
	}
	
	private static void listenForServerRequests() {
		// UDP server awaiting requests from other game servers
		DatagramSocket aSocket = null;
		try{
	    	aSocket = new DatagramSocket(UDP_BREAKER_PORT);
			byte[] buffer = new byte[65508];
 			while(true) {
 				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
  				aSocket.receive(request); 
  				String stringRequest = new String(request.getData(), 0, 7, StandardCharsets.UTF_8);
  				// get confirmation of successful break
  				if(stringRequest.equals("success")) {
  					System.out.println("Exiting as failure on replica two has been initiated");
  					System.exit(0);
  				}
    		}
		} catch (SocketException e){
			System.out.println("Socket Exception: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO Exception: " + e.getMessage());
		} finally {
			if(aSocket != null) aSocket.close();
		}
	}

}
