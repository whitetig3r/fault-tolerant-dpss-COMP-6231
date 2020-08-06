package replicatwo;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import exceptions.BadPasswordException;
import exceptions.BadUserNameException;
import exceptions.PlayerRemoveException;
import exceptions.TransferAccountException;
import exceptions.UnknownServerRegionException;
import frontend.GameServerPOA;
import models.Player;

public class GameServerServant extends GameServerPOA implements Runnable {
	private ArrayList<Integer> EXT_UDP_PORTS;
	private int INT_UDP_PORT;
	private final int SERVER_TIMEOUT_IN_MILLIS = 5000;
	private boolean hasCrashed = false;
	private DatagramSocket aSocket = null;
	private ORBThread orbThread;
	final String[] defaultORBArgs = { "-ORBInitialPort", "1050" };
	
	// INSTANCE-WIDE TRANSACTIONAL LOCKS
	private final WriteLock playerHashTransactionLock = new ReentrantReadWriteLock().writeLock();
	private final WriteLock loggerLock = new ReentrantReadWriteLock().writeLock();
	
	private ConcurrentHashMap<Character, CopyOnWriteArrayList<Player>> playerHash = new ConcurrentHashMap<>();

	private String gameServerLocation;
	private ORB orb;

	public GameServerServant(String location, ArrayList<Integer> extUdpPorts, int intUdpPort) throws UnknownServerRegionException {
		super();
		this.gameServerLocation = location; 
		this.EXT_UDP_PORTS = extUdpPorts;
		this.INT_UDP_PORT = intUdpPort;
		// create a region administrator account
		createPlayerAccount("Admin","Admin","Admin","Admin", getRegionDefaultIP(), 0);
		seedDataStore();
		orbThread = new ORBThread(createORB(defaultORBArgs));
		orbThread.start();
		System.out.println("ORB is running");
	}
	
	public GameServerServant(String gameServerLocation2, ArrayList<Integer> extUdpPorts, int iNT_UDP_PORT2,
			ConcurrentHashMap<Character, CopyOnWriteArrayList<Player>> playerHash2) {
		this.gameServerLocation = gameServerLocation2;
		this.EXT_UDP_PORTS = extUdpPorts;
		this.INT_UDP_PORT = iNT_UDP_PORT2;
		this.playerHash = playerHash2;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		 runRegionUdpServer();
	}
	
	protected void freeServerResources() {
		playerHash.clear();
		if(aSocket != null) aSocket.close();
	}
	
	// CORE PLAYER FUNCTIONALITY
	
	private void seedDataStore() throws UnknownServerRegionException {
		createPlayerAccount("Allen","White","whiteallen7","password", getRegionDefaultIP(), 23);
		createPlayerAccount("Bill","Johns","billy20","password", getRegionDefaultIP(), 48);
		createPlayerAccount("Crystal","Reigo","petula71","password", getRegionDefaultIP(), 35);
	}

	@Override
	public String createPlayerAccount(String fName, String lName, String uName, String password, String ipAddress, int age) {
		serverLog("Initiating CREATEACCOUNT for player", ipAddress);
		
		Character uNameFirstChar = uName.charAt(0);
		String retString = "ERR: An Error was encountered!";
		
		if(!this.playerHash.containsKey(uNameFirstChar)) {
			this.playerHash.putIfAbsent(uNameFirstChar, new CopyOnWriteArrayList<Player>());
		}
		
		try {
				playerHashTransactionLock.lock(); // LOCK
				
				Player playerToAdd = new Player(fName, lName, uName, password, ipAddress, age);
				
				Optional<Player> playerExists = this.playerHash.get(uNameFirstChar)
						.stream().filter(player -> player.getuName().equals(uName)).findAny();
				
				if(playerExists.isPresent()) {
					retString = "ERR: Player with that username already exists!";
				} else {
					this.playerHash.get(uNameFirstChar).addIfAbsent(playerToAdd);
					retString = String.format("Successfully created account for player with username -- '%s'", uName);
				}
				
				serverLog(retString, ipAddress);
		} catch(BadUserNameException | BadPasswordException e) {
			retString = e.getMessage();
			serverLog(retString, ipAddress);
		} finally {
			playerHashTransactionLock.unlock(); // UNLOCK
		}
		return retString; 
	}
	
	@Override
	public String playerSignIn(String uName, String password, String ipAddress) {
		Player playerToSignIn = null;
		serverLog("Initiating SIGNIN for player", ipAddress);
		Character uNameFirstChar = uName.charAt(0);
		
		if(!this.playerHash.containsKey(uNameFirstChar)) {
			String errExist = String.format("ERR: Player with username '%s' does not exist", uName);
			serverLog(errExist, ipAddress);
			return errExist;
		}
		
		try {
			playerToSignIn = this.playerHash.get(uNameFirstChar).stream().filter(player -> {
				return player.getuName().equals(uName) && player.getPassword().equals(password);
			}).findAny().orElse(null);
			
			if(playerToSignIn != null) {
				playerToSignIn.acquireLock(); // LOCK
				
				if(playerToSignIn.getStatus()) {
					String errSignedIn = String.format("ERR: Player '%s' is already signed in", uName); 
					serverLog(errSignedIn, ipAddress);
					return errSignedIn;
				} else {
					
					playerToSignIn.setStatus(true);
				}
				String success = String.format("Successfully signed in player with username -- '%s'",uName);
				serverLog(success, ipAddress);
				return success;
			}
		} finally {
			if(playerToSignIn != null && playerToSignIn.hasLock()) 
				playerToSignIn.releaseLock(); // UNLOCK
		}
		
		String errExist = String.format("ERR: Player with username '%s' and that password combination does not exist", uName);
		serverLog(errExist, ipAddress);
		return errExist;
	}
	
	@Override
	public String playerSignOut(String uName, String ipAddress) {
		Player playerToSignOut = null;
		serverLog("Initiating SIGNOUT for player", ipAddress);
		Character uNameFirstChar = uName.charAt(0);
		
		if(!this.playerHash.containsKey(uNameFirstChar)) {
			String errExist = String.format("ERR: Player with username '%s' does not exist", uName);
			serverLog(errExist, ipAddress);
			return errExist;
		}
		
		try {
			playerToSignOut = this.playerHash.get(uNameFirstChar).stream().filter(player -> {
				return player.getuName().equals(uName);
			}).findAny().orElse(null);
			
			if(playerToSignOut != null) {
				playerToSignOut.acquireLock(); // LOCK
				
				if(!(playerToSignOut.getStatus())) {
					String errSignedOut = String.format("ERR: Player '%s' is already signed out", uName);
					serverLog(errSignedOut, ipAddress);
					return errSignedOut;
				} else {
					
						playerToSignOut.setStatus(false);
					}
				String success = String.format("Successfully signed out player with username -- '%s'",uName);
				serverLog(success, ipAddress);
				return success;
			}
		} finally {
			if(playerToSignOut != null && playerToSignOut.hasLock()) 
				playerToSignOut.releaseLock(); // UNLOCK
		}
		
		String errExist = String.format("ERR: Player with username '%s' and that password combination does not exist", uName);
		serverLog(errExist, ipAddress);
		return errExist;
	}
	
	@Override
	public String transferAccount(String uName, String password, String oldIpAddress, String newIpAddress) {
		Player playerToTransfer = null;
		serverLog("Initiating TRANSFER ACCOUNT action for player", oldIpAddress);
		Character uNameFirstChar = uName.charAt(0);
		
		if(!this.playerHash.containsKey(uNameFirstChar)) {
			String errExist = String.format("ERR: Player with username '%s' does not exist", uName);
			serverLog(errExist, oldIpAddress);
			return errExist;
		}
		
		try {	
				playerToTransfer = this.playerHash.get(uNameFirstChar).stream().filter(player -> {
					return player.getuName().equals(uName) && player.getPassword().equals(password);
				}).findAny().orElse(null);
				
				boolean wasOnline = false;
				
				if(playerToTransfer != null) {
					try {
						playerToTransfer.acquireLock(); // LOCK
						
						playerToTransfer.setIpAddress(newIpAddress);
						
						if(playerToTransfer.getStatus()) {
							playerToTransfer.setStatus(false);
							wasOnline = true;
						}
						
						threadSafeRemovePlayer(playerToTransfer, uNameFirstChar);
						
						int ret = atomicallyExecuteTransfer(playerToTransfer, uNameFirstChar, newIpAddress);
						
						if(ret > 0) {
							String log = String.format("Successfully TRANSFERRED ACCOUNT for player with username %s to %s", uName, newIpAddress);
							serverLog(log, uName);
							return log;
						} else {
							throw new TransferAccountException();
						}
						
					} catch(PlayerRemoveException e) {
						String err = String.format("ERR: Failed to delete player with username -- %s because account does not exist. Aborting TRANSFER!", uName);
						serverLog(err, oldIpAddress);
						return err;
					} catch(TransferAccountException e) {
						String err = String.format("ERR: Failed to add player account with username %s on remote server. ROLLING BACK!", uName);
						playerToTransfer.setIpAddress(oldIpAddress);
						if(wasOnline) playerToTransfer.setStatus(true);
						threadSafeAddPlayerBack(playerToTransfer, uNameFirstChar);
						serverLog(err, oldIpAddress);
						return err;
					}
				} 
			} finally {
				if(playerToTransfer != null && playerToTransfer.hasLock()) 
					playerToTransfer.releaseLock(); // UNLOCK
			}
		
		String errExist = String.format("ERR: Player with username '%s' and that password combination does not exist", uName);
		serverLog(errExist, oldIpAddress);
		return errExist;
	}
	
	// END OF CORE PLAYER FUNCTIONALITY
	
	// CORE ADMIN FUNCTIONALITY
	
	@Override
	public String adminSignIn(String uName, String password, String ipAddress) {
		Player adminToSignIn = null;
		serverLog("Initiating SIGNIN for admin", ipAddress);
		Character uNameFirstChar = uName.charAt(0);
		
		if(uName.equals("Admin") && password.equals("Admin")) {
			
			try {
				adminToSignIn = this.playerHash.get(uNameFirstChar).stream().filter(player -> {
					return player.getuName().equals(uName) && player.getPassword().equals(password);
				}).findAny().orElse(null);
				
				if(adminToSignIn != null) {
					adminToSignIn.acquireLock(); // LOCK
					
					if(adminToSignIn.getStatus()) {
						String errSignedIn = "ERR: Admin is already signed in"; 
						serverLog(errSignedIn, ipAddress);
						return errSignedIn;
					} else {
							adminToSignIn.setStatus(true);
						}
				}
			} finally {
				if(adminToSignIn != null && adminToSignIn.hasLock()) 
					adminToSignIn.releaseLock(); // UNLOCK
			}
			
			String success = "Successfully signed in admin!";
			serverLog(success, ipAddress);
			return success;
				
		}
		
		String errExist = "ERR: Admin with that password combination does not exist";
		serverLog(errExist, ipAddress);
		return errExist;
	}
	
	@Override
	public String adminSignOut(String uName, String ipAddress) {
		Player adminToSignOut = null;
		serverLog("Initiating SIGNOUT for admin", ipAddress);
		Character uNameFirstChar = uName.charAt(0);
		
		if(uName.equals("Admin")) {
			try {
				adminToSignOut = this.playerHash.get(uNameFirstChar).stream().filter(player -> {
					return player.getuName().equals(uName);
				}).findAny().orElse(null);
				
				if(adminToSignOut != null) {
					adminToSignOut.acquireLock(); // LOCK
					if(!(adminToSignOut.getStatus())) {
						String errSignedOut = "ERR: Admin is already signed out";
						serverLog(errSignedOut, ipAddress);
						return errSignedOut;
					} else {
						adminToSignOut.setStatus(false);
					}
					
					String success = "Successfully signed out admin";
					serverLog(success, ipAddress);
					return success;
				}
			} finally {
				if(adminToSignOut != null && adminToSignOut.hasLock()) 
					adminToSignOut.releaseLock(); // UNLOCK
			}
		}
		
		String errExist = "ERR: Admin with that password combination does not exist";
		serverLog(errExist, ipAddress);
		return errExist;
	}
	
	@Override
	public String getPlayerStatus(String uName, String password, String ipAddress) {
		String retStatement = "ERR: Unrecognized Error while requesting player status!";
		
		if(!(uName.equals("Admin") && password.equals("Admin"))) {
			retStatement = "ERR: Incorrect credentials for Admin!";
		} else {
		
			Player admin = this.playerHash.get('A').stream().filter(player -> {
				return player.getuName().equals("Admin") && 
						player.getPassword().equals("Admin");
			}).findAny().orElse(null);
				
			if(admin != null) {
					String ret = retrievePlayerStatuses(ipAddress);
					serverLog(ret, ipAddress);
					return ret;
			}
		}
		
		serverLog(retStatement, ipAddress);
		return retStatement;
	}

	@Override
	public String suspendAccount(String uName, String password, String ipAddress, String uNameToSuspend) {
		Player playerToSuspend = null;
		serverLog("Initiating PLAYER ACCOUNT SUSPEND action for admin", ipAddress);
		Character uNameFirstChar = uNameToSuspend.charAt(0);
		
		if(uName.equals("Admin") && password.equals("Admin")) {
			try {
				playerToSuspend = this.playerHash.get(uNameFirstChar).stream().filter(player -> {
					return player.getuName().equals(uNameToSuspend);
				}).findAny().orElse(null);
				
				if(playerToSuspend != null) {
					playerToSuspend.acquireLock(); // LOCK
					
					Character firstCharOfPlayer = playerToSuspend.getuName().charAt(0);
					try {
						threadSafeRemovePlayer(playerToSuspend, firstCharOfPlayer);
					} catch(PlayerRemoveException e) {
						String err = String.format("ERR: Failed to delete player account with username %s..", uNameToSuspend);
						serverLog(err, "Admin");
						return err;
					}
					String success = String.format("Successfully suspended account for player with username -- %s", uNameToSuspend);
					serverLog(success, ipAddress);
					return success;
				} 
				else {
					String noSuchPlayer = String.format("ERR: Failed to find player account with username -- %s", uNameToSuspend);
					serverLog(noSuchPlayer, ipAddress);
					return noSuchPlayer;
				}
			} finally {
				if(playerToSuspend != null && playerToSuspend.hasLock())
					playerToSuspend.releaseLock(); // UNLOCK
			}
		}
		
		String errExist = "ERR: Admin with that password combination does not exist";
		serverLog(errExist, ipAddress);
		return errExist;
	}
	
	// END OF CORE ADMIN FUNCTIONALITY
	
	// UTILITIES AND HELPERS
	
	private String retrievePlayerStatuses(String ipAddress) {
	    CompletableFuture<String> intRetrieve = CompletableFuture.supplyAsync(()->{
			return getPlayerCounts();
	    });

	    CompletableFuture<String> extRetrieve1 = CompletableFuture.supplyAsync(()->{
	    	return makeUDPStatusRequestToExternalServer(EXT_UDP_PORTS.get(0));
	    });

	    CompletableFuture<String> extRetrieve2 = CompletableFuture.supplyAsync(()->{
	    	return makeUDPStatusRequestToExternalServer(EXT_UDP_PORTS.get(1));
	    });

	    CompletableFuture<Void> allRetrieve = CompletableFuture.allOf(intRetrieve, extRetrieve1, extRetrieve2); 
	    
	    try {
	        allRetrieve.get();
	        String retSucc = Stream.of(intRetrieve, extRetrieve1, extRetrieve2)
	        		.map(CompletableFuture::join)
	        		.collect(Collectors.joining("\n"));
	        serverLog(retSucc,ipAddress);
	        return retSucc;
	    } catch (Exception e) {
	    	e.printStackTrace();
		    String err = e.getMessage();
		    serverLog(err, ipAddress);
	    }
	    String err = "ERR: Could not retrieve player statuses!";
	    serverLog(err, ipAddress);
	    return err;
	}
	
	private String getPlayerCounts() {
		int online = 0;
		int offline = 0;
		try {
			playerHashTransactionLock.lock(); // LOCK
			for(Character index : this.playerHash.keySet()) {
				for(Player player : this.playerHash.get(index)) {
					if(player.getfName().equals("Admin")) continue;
					if(player.getStatus()) {
						online += 1;
					} else {
						offline += 1;
					}
				}
			}
		} finally {
			playerHashTransactionLock.unlock(); // UNLOCK
		}
		String succ = String.format("%s: Online: %d Offline: %d", this.gameServerLocation, online, offline);
		serverLog(succ, "Admin@"+this.gameServerLocation);
		return succ;
	}
	
	private int atomicallyExecuteTransfer(Player playerToTransfer, Character firstChar, String newIpAddress) throws TransferAccountException {
		byte[] serializedPlayer = serializePlayerObject(playerToTransfer);
		
		if(serializedPlayer != null) {
			int portToUse = getRegionUDPServerPort(newIpAddress);
			if(portToUse > 0) {
				String retVal = makeUDPTransferRequestToExternalServer(portToUse, serializedPlayer, playerToTransfer.getuName());
				if(retVal.startsWith("Successfully")){
					return 1;
				}
			} else {
				return -1;
			}
		}
		return -1;
	}
	
	private String addPlayerToServer(Player p) {
		return createPlayerAccount(p.getfName(), p.getlName(), p.getuName(), p.getPassword(), p.getIpAddress(), p.getAge());
	}
	
	private void threadSafeRemovePlayer(Player playerToSuspend, Character firstCharOfPlayer) throws PlayerRemoveException {
		if(this.playerHash.get(firstCharOfPlayer).contains(playerToSuspend)) {
			this.playerHash.get(firstCharOfPlayer).remove(playerToSuspend);
		} else {
			throw new PlayerRemoveException();
		}
	}
	
	private void threadSafeAddPlayerBack(Player playerToTransfer, Character uNameFirstChar) {
		// not calling createPlayer to avoid logging
		this.playerHash.get(uNameFirstChar).addIfAbsent(playerToTransfer);
	}
	
	// NETWORK UTILS 
	
	private void runRegionUdpServer() {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		
    	executorService.execute((Runnable) ()->{
    	  String log = String.format("Starting UDP Server for %s region on port %d ...",gameServerLocation, INT_UDP_PORT);
		  System.out.println(log);
		  serverLog("Admin",log);
		  listenForServerRequests();
		});
		
	}
	
	private void listenForServerRequests() {
		// UDP server awaiting requests from other game servers
		String loggingEntity = "Admin";
		try{
	    	aSocket = new DatagramSocket(INT_UDP_PORT);
			byte[] buffer = new byte[65508];
 			while(true) {
 				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
  				aSocket.receive(request); 
  				String toSend;
  				DatagramPacket reply; 
  				String stringRequest = new String(request.getData(), 0, 9, StandardCharsets.UTF_8);
  				// get status request
  				if(stringRequest.equals("getStatus")) {
	  				toSend = this.getPlayerCounts();
	  				loggingEntity = "Admin";
  				} 
  				// transfer player request
  				else {
  					Player playerToAdd = deserializePlayer(request.getData());
  					toSend = addPlayerToServer(playerToAdd);
  					loggingEntity = playerToAdd.getuName();
  				}
  				reply = new DatagramPacket(toSend.getBytes(), toSend.getBytes().length, request.getAddress(), request.getPort());
    			aSocket.send(reply);
    		}
		} catch (SocketException e){
			hasCrashed = true;
			System.out.println("Socket Exception: " + e.getMessage());
			serverLog(e.getMessage(), loggingEntity);
		} catch (IOException e) {
			hasCrashed = true;
			System.out.println("IO Exception: " + e.getMessage());
			serverLog(e.getMessage(), loggingEntity);
		} catch (TransferAccountException e) {
			hasCrashed = true;
			System.out.println("Transfer Account Exception: " + e.getMessage());
			serverLog(e.getMessage(), loggingEntity);
		} finally {
			if(aSocket != null) aSocket.close();
		}
	}
	
	private byte[] serializePlayerObject(Player playerToTransfer) throws TransferAccountException {
		byte[] serializedPlayer = null;
		try {
			ByteArrayOutputStream tempByteOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream tempObjectOutputStream = new ObjectOutputStream(tempByteOutputStream);
			tempObjectOutputStream.writeObject(playerToTransfer);
			serializedPlayer = tempByteOutputStream.toByteArray();
		} catch (IOException e) {
			throw new TransferAccountException();
		}
		return serializedPlayer;
	}

	private Player deserializePlayer(byte[] player) throws TransferAccountException {
		ByteArrayInputStream bis = new ByteArrayInputStream(player);
		ObjectInput in;
		Player playerToReturn = null;
		try {
			in = new ObjectInputStream(bis);
			playerToReturn = (Player) in.readObject();
		} catch (IOException | ClassNotFoundException e) {
			throw new TransferAccountException();
		}
		return playerToReturn;
	}

	private String makeUDPStatusRequestToExternalServer(int serverPort) {
		DatagramSocket aSocket = null;
		String reqOp = "getStatus";
		try {
			aSocket = new DatagramSocket();    
			aSocket.setSoTimeout(SERVER_TIMEOUT_IN_MILLIS); 
			byte [] m = reqOp.getBytes();
			InetAddress aHost = InetAddress.getByName("127.0.0.1");		                                                 
			DatagramPacket request =
			 	new DatagramPacket(m, reqOp.length(), aHost, serverPort);
			aSocket.send(request);			                        
			byte[] buffer = new byte[1000];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);	
			aSocket.receive(reply);
			String succ = new String(reply.getData());	
			serverLog(succ, "Admin");
			return succ;
		} catch (SocketTimeoutException e) {
			String timeOut = String.format("ERR: Request to server on port %d has timed out!", serverPort);
			serverLog(timeOut, "Admin");
			return timeOut;
		} catch (SocketException e){
			serverLog(e.getMessage(), "Admin");
			return "ERR: " + e.getMessage();
		} catch (IOException e) {
			serverLog(e.getMessage(), "Admin");
			return "ERR: " + e.getMessage();
		} finally {
			if(aSocket != null) aSocket.close();
		}
	}
	
	private String makeUDPTransferRequestToExternalServer(int serverPort, byte[] serializedPlayer, String playerUsername) {
		DatagramSocket aSocket = null;
		try {
			aSocket = new DatagramSocket();    
			aSocket.setSoTimeout(SERVER_TIMEOUT_IN_MILLIS); 
			byte [] m = serializedPlayer;
			InetAddress aHost = InetAddress.getByName("127.0.0.1");		                                                 
			DatagramPacket request =
			 	new DatagramPacket(m, m.length, aHost, serverPort);
			aSocket.send(request);			                        
			byte[] buffer = new byte[1000];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);	
			aSocket.receive(reply);
			String succ = new String(reply.getData());	
			serverLog(succ, playerUsername);
			return succ;
		} catch (SocketTimeoutException e) {
			String timeOut = String.format("ERR: Request to server on port %d has timed out!", serverPort);
			serverLog(timeOut, playerUsername);
			return timeOut;
		} catch (SocketException e){
			serverLog(e.getMessage(), playerUsername);
			return "Socket Exception: " + e.getMessage();
		} catch (IOException e) {
			serverLog(e.getMessage(), playerUsername);
			return "IO Exception: " + e.getMessage();
		} finally {
			if(aSocket != null) aSocket.close();
		}
	}
	
	// END OF NETWORK UTILS
	
	private void setExternalPorts() throws UnknownServerRegionException {
		switch(this.gameServerLocation) {
			case "NA": {
				INT_UDP_PORT = Integer.valueOf(EXT_UDP_PORTS.get(0));
				EXT_UDP_PORTS.remove(0);
				break;
			}
			case "EU": {
				INT_UDP_PORT = Integer.valueOf(EXT_UDP_PORTS.get(1));
				EXT_UDP_PORTS.remove(1);
				break;
			}
			case "AS": {
				INT_UDP_PORT = Integer.valueOf(EXT_UDP_PORTS.get(2));
				EXT_UDP_PORTS.remove(2);
				break;
			}
			default:
				throw new UnknownServerRegionException();
		}
	}

	private String getRegionDefaultIP() throws UnknownServerRegionException {
		switch(gameServerLocation) {
			case "NA":
				return "132.168.2.22";
			case "EU":
				return "93.168.2.22";
			case "AS":
				return "182.168.2.22";
			default:
				throw new UnknownServerRegionException();
		}
	}
	
	private int getRegionUDPServerPort(String ipAddress) {
		if(ipAddress.startsWith("132")) {
			return getUDPServerPort("NA");
		} else if (ipAddress.startsWith("93")) {
			return getUDPServerPort("EU");
		} else if (ipAddress.startsWith("182")) {
			return getUDPServerPort("AS");
		} else return -1;
	}

	private int getUDPServerPort(String region) {
		int port = -1;
		System.out.println("REGION -- " + region + " Ports -- " + EXT_UDP_PORTS.stream().map(Object::toString).collect(Collectors.joining(",")));
		switch(region) {
			case "NA": {
				port = EXT_UDP_PORTS.get(0);
				break;
			} 
			case "EU": {
				if(gameServerLocation.equals("NA")) {
					port = EXT_UDP_PORTS.get(0);
				} else {
					port = EXT_UDP_PORTS.get(1);
				}
				break;
			}
			case "AS": {
				if(gameServerLocation.equals("NA")) {
					port = EXT_UDP_PORTS.get(1);
				} else {
					port = EXT_UDP_PORTS.get(1);
				}
				break;
			}
		}
		return port;
	}

	private void serverLog(String logStatement, String ipAddress) {
		 DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
		 LocalDateTime tStamp = LocalDateTime.now(); 
		 String writeString = String.format("[%s] Response to %s -- %s", dtf.format(tStamp), ipAddress, logStatement);
		 try{
			 
			 loggerLock.lock(); // LOCK
			 
			 File file = new File(String.format("server_logs/%s-server.log", this.gameServerLocation));
			 file.getParentFile().mkdirs();
			 FileWriter fw = new FileWriter(file, true);
			 BufferedWriter logger = new BufferedWriter(fw);
			 logger.write(writeString);
			 logger.newLine();
			 logger.close();
			 
		} catch (IOException e) {
			// can't really log an error while logging
			e.printStackTrace();
		} finally {
			loggerLock.unlock(); // UNLOCK
		}
	}

	// CORBA UTILS 
	
	public void setORB(ORB orb) {
		this.orb = orb; 
	}
	
	private ORB createORB(String[] pArgs)
	{
		orb = null;
		try {
			// Initialize the ORB object
			orb = ORB.init(pArgs, null);
			POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			GameServerServant aInterface = new GameServerServant(gameServerLocation, EXT_UDP_PORTS, INT_UDP_PORT, playerHash);
			byte [] id = rootPOA.activate_object(aInterface);
			// Obtain reference to CORBA object
			org.omg.CORBA.Object reference_CORBA = rootPOA.id_to_reference(id);
			// Write the CORBA object to a file
			String stringORB = orb.object_to_string(reference_CORBA);
			PrintWriter file = new PrintWriter(gameServerLocation + "_BIOR.txt");
			file.print(stringORB);
			file.close();
			rootPOA.the_POAManager().activate();
			System.out.println("ORB init completed with file " + gameServerLocation + "_BIOR.txt");
		} catch (ServantAlreadyActive | WrongPolicy | 
				ObjectNotActive | FileNotFoundException | AdapterInactive | org.omg.CORBA.ORBPackage.InvalidName e) {
			System.out.println("ORB Creation Error: " + e.getMessage());
		}
		return orb;
	}
	
	public void shutdown() {
		orb.shutdown(false);
	}
	
}
