package frontend;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

public class GameServerFrontEnd extends GameServerPOA implements Runnable {
	private static Queue <List<Object>> fifoQueue;
	
	private final int UDP_PORT_FRONTEND = 6000;
	private final int UDP_PORT_LEAD = 4000;
	
	private static enum ACTION_TO_PERFORM {
		  PLAYER_CREATE_ACCOUNT,
		  PLAYER_SIGN_IN,
		  PLAYER_SIGN_OUT,
		  PLAYER_TRANSFER_ACCOUNT,
		  ADMIN_SIGN_IN,
		  ADMIN_SIGN_OUT,
		  ADMIN_GET_PLAYER_STATUS, 
		  ADMIN_SUSPEND_PLAYER_ACCOUNT
	}
	
	private static GameServerORBThread orbThread;
	private static GameServerFEThread udpThread;

	private DatagramSocket sendSocket;
	private DatagramPacket requestToReplicaLeader;
	private byte [] message;
	private String data;
	private InetAddress host;
	
	private GameServerFrontEnd(String[] args) {
		fifoQueue = new LinkedList <List<Object>>();
		try {
			sendSocket = new DatagramSocket();
			host = InetAddress.getByName("localhost");
		} catch (SocketException | UnknownHostException e) {
			// log
		}
		orbThread = new GameServerORBThread(createORB(args));
		orbThread.start();
		udpThread = new GameServerFEThread(UDP_PORT_FRONTEND);
		udpThread.start();
	}
	
	public static void main(String[] args) 
	{
		final String[] defaultORBArgs = { "-ORBInitialPort", "1050" };
		new Thread(new GameServerFrontEnd(defaultORBArgs)).start();
	}
	
	public GameServerFrontEnd() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void run() 
	{
		System.out.println("Starting FE Core Thread");
		List<Object> tmpList;
		while(true)
		{
			if(!fifoQueue.isEmpty()) {
				tmpList = fifoQueue.remove();
				sendRequestToReplicaLeader(tmpList);
			}
			
			if(udpThread.hasCrashed())
			{
				udpThread = new GameServerFEThread(UDP_PORT_FRONTEND);
				udpThread.start();
			}
			
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private ORB createORB(String[] args)
	{
		ORB orb = null;
		try{
			  final String[] defaultORBArgs = { "-ORBInitialPort", "1050" };
			  args = args.length == 0 ? defaultORBArgs : args;
		      orb = ORB.init(args, null);
	
		      POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
		      GameServerFrontEnd feInterface = new GameServerFrontEnd();
		      byte[] objectId = rootpoa.activate_object(feInterface);
	
		      org.omg.CORBA.Object ref = rootpoa.id_to_reference(objectId);
		      String stringifiedORB = orb.object_to_string(ref);
			  PrintWriter file = new PrintWriter("FE" + "_IOR.txt");
			  file.print(stringifiedORB);
			  file.close();
		          
		      rootpoa.the_POAManager().activate();
		} catch (Exception e) {
		    System.err.println("ERROR: " + e);
		    e.printStackTrace(System.out);
		}
		return orb;
	}
	
	private void sendRequestToReplicaLeader(List <Object> requestArgs)
	{	
		ACTION_TO_PERFORM actionToPerform = (ACTION_TO_PERFORM)requestArgs.get(0);
		switch(actionToPerform) {
			case PLAYER_CREATE_ACCOUNT:;
			case PLAYER_SIGN_IN:
			case PLAYER_SIGN_OUT:
			case PLAYER_TRANSFER_ACCOUNT:
			case ADMIN_SIGN_IN:
			case ADMIN_SIGN_OUT:
			case ADMIN_GET_PLAYER_STATUS:
			case ADMIN_SUSPEND_PLAYER_ACCOUNT:
				packageAndDispatchRequest(actionToPerform, requestArgs.subList(1, requestArgs.size()));
		}
		
	}
	
	private void packageAndDispatchRequest(ACTION_TO_PERFORM action, List<Object>argList) {
		try {	
			List<Object>tempList = new ArrayList<Object>();
			tempList.add("FE");
			tempList.add(action.name());
			tempList.addAll(argList);
			data = tempList.stream() 
                    .map(String::valueOf) 
                    .collect(Collectors.joining("/")); 
			
			message = data.getBytes();
			requestToReplicaLeader = new DatagramPacket(message, data.length(), host, UDP_PORT_LEAD);
			sendSocket.send(requestToReplicaLeader);
		} catch (SocketException e) {
			// LOG
		} catch (IOException e) {
			// LOG
		}
	}
	

	@Override
	public String createPlayerAccount(String fName, String lName, String uName, String password, String ipAddress,
			int age) {
		GameServerORBThread.setLeaderResponded(false);
		List<Object> tempList = new ArrayList<Object>();
		tempList.addAll(Arrays.asList(ACTION_TO_PERFORM.PLAYER_CREATE_ACCOUNT,fName,lName,uName,password,ipAddress,age));
		fifoQueue.add(tempList);
		tempList = null;
		while(!GameServerORBThread.hasLeaderResponded())  /* Wait for the leader to respond */
			System.out.print("");
		// LOG
		return "log message";
	}

	@Override
	public String playerSignIn(String uName, String password, String ipAddress) {
		GameServerORBThread.setLeaderResponded(false);
		List<Object> tempList = new ArrayList<Object>();
		tempList.addAll(Arrays.asList(ACTION_TO_PERFORM.PLAYER_SIGN_IN,uName,password,ipAddress));
		fifoQueue.add(tempList);
		tempList = null;
		while(!GameServerORBThread.hasLeaderResponded())  /* Wait for the leader to respond */
			System.out.print("");
		// LOG
		return "log message";
	}

	@Override
	public String playerSignOut(String uName, String ipAddress) {
		GameServerORBThread.setLeaderResponded(false);
		List<Object> tempList = new ArrayList<Object>();
		tempList.addAll(Arrays.asList(ACTION_TO_PERFORM.PLAYER_SIGN_OUT,uName,ipAddress));
		fifoQueue.add(tempList);
		tempList = null;
		while(!GameServerORBThread.hasLeaderResponded())  /* Wait for the leader to respond */
			System.out.print("");
		// LOG
		return "log message";
	}

	@Override
	public String adminSignIn(String uName, String password, String ipAddress) {
		GameServerORBThread.setLeaderResponded(false);
		List<Object> tempList = new ArrayList<Object>();
		tempList.addAll(Arrays.asList(ACTION_TO_PERFORM.ADMIN_SIGN_IN,uName,password,ipAddress));
		fifoQueue.add(tempList);
		tempList = null;
		while(!GameServerORBThread.hasLeaderResponded())  /* Wait for the leader to respond */
			System.out.print("");
		// LOG
		return "log message";
	}

	@Override
	public String adminSignOut(String uName, String ipAddress) {
		GameServerORBThread.setLeaderResponded(false);
		List<Object> tempList = new ArrayList<Object>();
		tempList.addAll(Arrays.asList(ACTION_TO_PERFORM.ADMIN_SIGN_OUT,uName,ipAddress));
		fifoQueue.add(tempList);
		tempList = null;
		while(!GameServerORBThread.hasLeaderResponded())  /* Wait for the leader to respond */
			System.out.print("");
		// LOG
		return "log message";
	}

	@Override
	public String getPlayerStatus(String uName, String password, String ipAddress) {
		GameServerORBThread.setLeaderResponded(false);
		List<Object> tempList = new ArrayList<Object>();
		tempList.addAll(Arrays.asList(ACTION_TO_PERFORM.ADMIN_GET_PLAYER_STATUS,uName,password,ipAddress));
		fifoQueue.add(tempList);
		tempList = null;
		while(!GameServerORBThread.hasLeaderResponded())  /* Wait for the leader to respond */
			System.out.print("");
		// LOG
		return "log message";
	}

	@Override
	public String transferAccount(String uName, String password, String oldIpAddress, String newIpAddress) {
		GameServerORBThread.setLeaderResponded(false);
		List<Object> tempList = new ArrayList<Object>();
		tempList.addAll(Arrays.asList(ACTION_TO_PERFORM.PLAYER_TRANSFER_ACCOUNT,uName,password));
		fifoQueue.add(tempList);
		tempList = null;
		while(!GameServerORBThread.hasLeaderResponded())  /* Wait for the leader to respond */
			System.out.print("");
		// LOG
		return "log message";
	}

	@Override
	public String suspendAccount(String uName, String password, String ipAddress, String uNameToSuspend) {
		GameServerORBThread.setLeaderResponded(false);
		List<Object> tempList = new ArrayList<Object>();
		tempList.addAll(Arrays.asList(ACTION_TO_PERFORM.ADMIN_SUSPEND_PLAYER_ACCOUNT,uName,password,ipAddress));
		fifoQueue.add(tempList);
		tempList = null;
		while(!GameServerORBThread.hasLeaderResponded())  /* Wait for the leader to respond */
			System.out.print("");
		// LOG
		return "log message";
	}

}
