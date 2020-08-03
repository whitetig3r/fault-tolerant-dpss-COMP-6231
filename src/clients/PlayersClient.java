package clients;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import frontend.GameServer;
import frontend.GameServerHelper;

public class PlayersClient extends CoreClient {

	private static Scanner sc = new Scanner(System.in);
	private static GameServer serverStub; 
	private static String[] CLIENT_ORB_ARGS;
	
	
	public static void main(String[] args) throws InvalidName, NotFound, CannotProceed, org.omg.CosNaming.NamingContextPackage.InvalidName {
		final String[] defaultORBArgs = { "-ORBInitialPort", "1050" };
		CLIENT_ORB_ARGS = args.length == 0 ? defaultORBArgs : args;
		setFEORB();
		
		final String MENU_STRING = "\n-- Player Client CLI --\n"
				+ "Pick an option ...\n"
				+ "1. Create a Player account\n"
				+ "2. Sign a Player in\n"
				+ "3. Sign a Player out\n"
				+ "4. Transfer Player account\n"
				+ "5. Exit the CLI\n"
				+ "--------------------------\n";
		try {
			System.out.println("NOTE -- Player Logs available at " + System.getProperty("user.dir") + "/player_logs/");
			System.out.println("Seeded Accounts -- \"whiteallen7\" , "
					+ "\"billy20\" , \"petula71\" -- "
					+ "Password for all these accounts is \"password\"");
			while(true) {
				System.out.println(MENU_STRING);
				switch(sc.nextLine()) {
					case "1": {
						createPlayerAccount();
						break;
					}
					case "2": {
						playerSignIn();
						break;
					}
					case "3": {
						playerSignOut();
						break;
					}
					case "4": {
						playerTransferAccount();
						break;
					}
					case "5": {
						System.out.println("Goodbye!");
						System.exit(0);
					}
					default: {
						System.out.println("Invalid Option selected!");
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	private static void setFEORB() throws InvalidName, NotFound, CannotProceed, org.omg.CosNaming.NamingContextPackage.InvalidName {
	    // create and initialize the ORB
	    ORB orb = ORB.init(CLIENT_ORB_ARGS, null);
		String stringORB = "";
		
		try {
		    BufferedReader bufferedReader = new BufferedReader(new FileReader("FE" + "_IOR.txt"));
			stringORB = bufferedReader.readLine();
			bufferedReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		org.omg.CORBA.Object reference_CORBA = orb.string_to_object(stringORB);
		serverStub = GameServerHelper.narrow(reference_CORBA);
		System.out.println("HERE " + serverStub);
	}

	private static void createPlayerAccount() {
		String fName;
		String lName;
		String uName;
		String password;
		String ipAddress;
		int age;
		
		setLoggingContext("UNRESOLVED_PLAYER", "UnresolvedIP");
		fName = getSafeStringInput("Enter First Name:");
		lName = getSafeStringInput("Enter Last Name:");
		uName = getSafeStringInput("Enter User Name:");
		password = getSafeStringInput("Enter Password:");
		age = getSafeIntInput("Enter Age:");
		
		System.out.println("Enter IP Address:");
		ipAddress = getIpAddressInput();
		
		try {
			realizeCreatePlayerAccount(fName, lName, uName, password, age, ipAddress);
		} catch(InvalidName | NotFound | CannotProceed | org.omg.CosNaming.NamingContextPackage.InvalidName e) {
			String err = "ERROR: CORBA services encountered an error";
			System.out.println(err);
			playerLog(err, uName, ipAddress);
		} catch (org.omg.CORBA.SystemException e) {
			handleServerDown(uName, ipAddress, e);
		}
	}
		
	private static void playerSignIn() {
		String uName;
		String password;
		String ipAddress;
		
		setLoggingContext("UNRESOLVED_PLAYER", "UnresolvedIP");
		uName = getSafeStringInput("Enter User Name:");
		password = getSafeStringInput("Enter Password:");
		System.out.println("Enter IP Address:");
		ipAddress = getIpAddressInput();
		try {
			realizePlayerSignIn(uName, password, ipAddress);
		} catch(InvalidName | NotFound | CannotProceed | org.omg.CosNaming.NamingContextPackage.InvalidName e) {
			String err = "ERROR: CORBA services encountered an error";
			System.out.println(err);
			playerLog(err, uName, ipAddress);
		} catch (org.omg.CORBA.SystemException e) {
			handleServerDown(uName, ipAddress, e);
		}

	}
	
	private static void playerSignOut() {
		String uName;
		String ipAddress;
		
		setLoggingContext("UNRESOLVED_PLAYER", "UnresolvedIP");
		uName = getSafeStringInput("Enter User Name:");
		System.out.println("Enter IP Address:");
		ipAddress = getIpAddressInput();
		try {
			realizePlayerSignOut(uName, ipAddress);
		} catch(InvalidName | NotFound | CannotProceed | org.omg.CosNaming.NamingContextPackage.InvalidName e) {
			String err = "ERROR: CORBA services encountered an error";
			System.out.println(err);
			playerLog(err, uName, ipAddress);
		} catch (org.omg.CORBA.SystemException e) {
			handleServerDown(uName, ipAddress, e);
		}

	}
	
	private static void playerTransferAccount() {
		String uName;
		String password;
		String oldIpAddress;
		String newIpAddress = "";
		
		setLoggingContext("UNRESOLVED_PLAYER", "UnresolvedIP");
		uName = getSafeStringInput("Enter User Name:");
		password = getSafeStringInput("Enter Password:");
		System.out.println("Enter old IP Address:");
		oldIpAddress = getIpAddressInput();
		System.out.println("Enter new IP Address:");
		do {
			System.out.println("NOTE: Ensure that the region of the IP Address to Transfer is different from your current region!");
			newIpAddress = getIpAddressInput();
		} while(newIpAddress.startsWith(oldIpAddress.substring(0,3)));
		
		try {
			realizePlayerTransferAccount(uName, password, oldIpAddress, newIpAddress);
		} catch(InvalidName | NotFound | CannotProceed | org.omg.CosNaming.NamingContextPackage.InvalidName e) {
			String err = "ERROR: CORBA services encountered an error";
			System.out.println(err);
			playerLog(err, uName, oldIpAddress);
		} catch (org.omg.CORBA.SystemException e) {
			handleServerDown(uName, oldIpAddress, e);
		}

	}
	
	private static void realizePlayerTransferAccount(String uName, String password, String oldIpAddress,
			String newIpAddress) throws InvalidName, NotFound, CannotProceed, org.omg.CosNaming.NamingContextPackage.InvalidName {
		String retStatement = serverStub.transferAccount(uName, password, oldIpAddress, newIpAddress);
		System.out.println(retStatement);
		playerLog(retStatement, uName, oldIpAddress);
		
	}

	private static void realizeCreatePlayerAccount(String fName, String lName, String uName, String password, int age, String ipAddress) throws InvalidName, NotFound, CannotProceed, org.omg.CosNaming.NamingContextPackage.InvalidName { 
		String retStatement = serverStub.createPlayerAccount(fName, lName, uName, password, ipAddress, age);
		System.out.println(retStatement);
		playerLog(retStatement, uName, ipAddress);	
	}
	
	private static void realizePlayerSignIn(String uName, String password, String ipAddress) throws InvalidName, NotFound, CannotProceed, org.omg.CosNaming.NamingContextPackage.InvalidName {
		String retStatement = serverStub.playerSignIn(uName, password, ipAddress);
		System.out.println(retStatement);
		playerLog(retStatement, uName, ipAddress);
	}

	private static void realizePlayerSignOut(String uName, String ipAddress) throws InvalidName, NotFound, CannotProceed, org.omg.CosNaming.NamingContextPackage.InvalidName {
		String retStatement = serverStub.playerSignOut(uName, ipAddress);
		System.out.println(retStatement);
		playerLog(retStatement, uName, ipAddress);
	}
	
	private static void handleServerDown(String uName, String ipAddress, Exception e) {
		String err = "ERROR: Region server is not active";
		System.out.println(err);
		playerLog(err, uName, ipAddress);
	}

}