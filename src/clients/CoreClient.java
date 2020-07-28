package clients;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class CoreClient {
	private static String uName;
	private static String ipAddress;
	private static String serverToConnect;
	private static boolean isAdmin;
	
	private static Scanner sc = new Scanner(System.in);
	
	protected static void setLoggingContext(String suppliedUName, String suppliedIpAddress) {
		isAdmin = false;
		uName = suppliedUName;
		ipAddress = suppliedIpAddress;
	}
	
	protected static void setLoggingContext(String suppliedUName, String suppliedServerToConnect, boolean _isAdmin) {
		isAdmin = _isAdmin;
		uName = suppliedUName;
		serverToConnect = suppliedServerToConnect;
	}
	
	private static boolean isValidIP(String ipAddr) {
		if(ipAddr == null) return false;
		
		String[] groups = ipAddr.split("\\.");

		if (groups.length != 4) {
			return false;
		}
		
		return true;
	}
	
	protected static String getRegionServer(String ipAddress) {
		if(isValidIP(ipAddress)) {
			if(ipAddress.startsWith("132")) {
				return "GameServerNA";
			} else if (ipAddress.startsWith("93")) {
				return "GameServerEU";
			} else if (ipAddress.startsWith("182")) {
				return "GameServerAS";
			} else return "Unknown Server";
		} 
		return null;
	}
	
	
	protected static String getIpAddressInput() {
		
		String ipAddress = sc.nextLine();
		String regionServer;
		while ((regionServer = CoreClient.getRegionServer(ipAddress)) == null || !regionServer.startsWith("GameServer")) {
			if(regionServer != null && regionServer.equals("Unknown Server")) {
				String err = "ERROR: Server for this region does not exist!";
				System.out.println(err);
				if(isAdmin) adminLog(err,uName,serverToConnect);
				else playerLog(err, uName, ipAddress);
				
			} else {
				String err = "ERROR: IP Address in invalid!";
				System.out.println(err);
				if(isAdmin) adminLog(err,uName,serverToConnect);
				else playerLog(err, uName, ipAddress);
			}
			System.out.println("Enter IP Address:");
			ipAddress = sc.nextLine();
		}
		return ipAddress;
	}
	
	protected static String getSafeStringInput(String prompt) {
		while(true) {
			System.out.println(prompt);
			String inpVal = sc.nextLine();
			if(inpVal.length() == 0) {
				String err = "ERROR: Empty input!";
				System.out.println(err);
				if(isAdmin) adminLog(err,uName,serverToConnect);
				else playerLog(err, uName, ipAddress);
				continue;
			}
			if(inpVal.trim().length() < inpVal.length()) {
				String err = "ERROR: Cannot contain leading or trailing spaces!";
				System.out.println(err);
				if(isAdmin) adminLog(err,uName,serverToConnect);
				else playerLog(err, uName, ipAddress);
				continue;
			}
			return inpVal;
		}
		
	}
	
	protected static int getSafeIntInput(String prompt) {
		while(true) {
			try {
				System.out.println(prompt);
				int inpVal = Integer.parseInt(sc.nextLine());
				return inpVal;
			} catch (NumberFormatException e) {
				String err = "ERROR: Not a valid number!";
				System.out.println(err);
				if(isAdmin) adminLog(err,uName,serverToConnect);
				else playerLog(err, uName, ipAddress);
			    continue;
			}
		}
		
	}
	
	protected static synchronized void playerLog(String logStatement, String uName, String ipAddress) {
		 DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
		 LocalDateTime tStamp = LocalDateTime.now(); 
		 String writeString = String.format("[%s] %s @ (%s) -- %s", dtf.format(tStamp), uName, ipAddress, logStatement);
		 String serverRegion = getRegionServer(ipAddress);
		 String fName = serverRegion == null || serverRegion.equals("Unknown Server") ? 
				 "UNRESOLVED-Players" : serverRegion.substring(10);
		 try{
			File file = new File(String.format("player_logs/%s/%s.log", fName, uName));
			file.getParentFile().mkdirs();
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter logger = new BufferedWriter(fw);
			logger.write(writeString);
			logger.newLine();
			logger.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected static synchronized void adminLog(String logStatement, String uName, String serverToConnect) {
		 ArrayList<String> REGION_LIST = new ArrayList<String>(Arrays.asList("GameServerNA","GameServerEU","GameServerAS"));
		 if(!REGION_LIST.contains(serverToConnect)) serverToConnect = "GameServerUNRESOLVED";
		 DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
		 LocalDateTime tStamp = LocalDateTime.now(); 
		 String writeString = String.format("[%s] %s @ (Admin-%s) -- %s", dtf.format(tStamp), uName, serverToConnect.substring(10), logStatement);
		 try{
			File file = new File(String.format("admin_logs/%s-admin.log", serverToConnect.substring(10)));
			file.getParentFile().mkdirs();
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter logger = new BufferedWriter(fw);
			logger.write(writeString);
			logger.newLine();
			logger.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
}
