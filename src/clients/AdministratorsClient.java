package clients;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import exceptions.UnknownServerRegionException;
import frontend.GameServer;
import frontend.GameServerHelper;

public class AdministratorsClient extends CoreClient {

  private static Scanner sc = new Scanner(System.in);
  private static GameServer serverStub;
  private static String[] CLIENT_ORB_ARGS;


  public static void main(String[] args) throws InvalidName, NotFound, CannotProceed,
      org.omg.CosNaming.NamingContextPackage.InvalidName {
    final String[] defaultORBArgs = {"-ORBInitialPort", "1050"};
    CLIENT_ORB_ARGS = args.length == 0 ? defaultORBArgs : args;
    setFEORB();
    System.out.println(
        "NOTE -- Admin Logs available at " + System.getProperty("user.dir") + "/admin_logs");
    final String MENU_STRING = "\n-- Admin Client CLI --\n" + "Pick an option ...\n"
        + "1. Sign in with admin privileges\n" + "2. Sign out admin\n"
        + "3. Get status of all players playing the game\n" + "4. Suspend a Player account\n"
        + "5. Exit the CLI\n" + "--------------------------\n";
    while (true) {
      System.out.println(MENU_STRING);
      switch (sc.nextLine()) {
        case "1": {
          adminSignIn();
          break;
        }
        case "2": {
          adminSignOut();
          break;
        }
        case "3": {
          adminGetPlayerStatus();
          break;
        }
        case "4": {
          adminSuspendAccount();
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
  }

  private static void adminSignIn() {
    String uName;
    String password;
    String ipAddress;

    setLoggingContext("UNRESOLVED", "UnresolvedIP", true);
    uName = getSafeStringInput("Enter User Name:");
    password = getSafeStringInput("Enter Password:");
    System.out.println("Enter IP Address:");
    ipAddress = getIpAddressInput();

    try {
      realizeAdminSignIn(uName, password, ipAddress);
    } catch (InvalidName | NotFound | CannotProceed
        | org.omg.CosNaming.NamingContextPackage.InvalidName e) {
      String err = "ERROR: CORBA services encountered an error";
      System.out.println(err);
      playerLog(err, uName, ipAddress);
    } catch (org.omg.CORBA.SystemException e) {
      handleServerDown(uName, ipAddress, e);
    } catch (UnknownServerRegionException e) {
      String err = "ERROR: Unknown Server for IP address!";
      System.out.println(err);
      adminLog(err, uName, "Unknown Server");
    }

  }

  private static void adminSignOut() {
    String uName;
    String ipAddress;

    setLoggingContext("UNRESOLVED", "UnresolvedIP", true);
    uName = getSafeStringInput("Enter User Name:");
    System.out.println("Enter IP Address:");
    ipAddress = getIpAddressInput();

    try {
      realizeAdminSignOut(uName, ipAddress);
    } catch (InvalidName | NotFound | CannotProceed
        | org.omg.CosNaming.NamingContextPackage.InvalidName e) {
      String err = "ERROR: CORBA services encountered an error";
      System.out.println(err);
      playerLog(err, uName, ipAddress);
    } catch (org.omg.CORBA.SystemException e) {
      handleServerDown(uName, ipAddress, e);
    } catch (UnknownServerRegionException e) {
      String err = "ERROR: Unknown Server for IP address!";
      System.out.println(err);
      adminLog(err, uName, "Unknown Server");
    }

  }

  private static void adminGetPlayerStatus() {
    String uName;
    String password;
    String ipAddress;

    setLoggingContext("UNRESOLVED", "UnresolvedIP", true);
    uName = getSafeStringInput("Enter User Name:");
    password = getSafeStringInput("Enter Password:");
    System.out.println("Enter IP Address:");
    ipAddress = getIpAddressInput();

    try {
      realizeAdminGetPlayerStatus(uName, password, ipAddress);
    } catch (InvalidName | NotFound | CannotProceed
        | org.omg.CosNaming.NamingContextPackage.InvalidName e) {
      String err = "ERROR: CORBA services encountered an error";
      System.out.println(err);
      adminLog(err, uName, getRegionServer(ipAddress));
    } catch (org.omg.CORBA.SystemException e) {
      handleServerDown(uName, ipAddress, e);
    } catch (UnknownServerRegionException e) {
      String err = "ERROR: Unknown Server for IP address!";
      System.out.println(err);
      adminLog(err, uName, "Unknown Server");
    }

  }

  private static void adminSuspendAccount() {
    String uName;
    String uNameToSuspend;
    String password;
    String ipAddress;

    setLoggingContext("UNRESOLVED", "UnresolvedIP", true);
    uName = getSafeStringInput("Enter User Name:");
    password = getSafeStringInput("Enter Password:");
    System.out.println("Enter IP Address:");
    ipAddress = getIpAddressInput();
    uNameToSuspend = getSafeStringInput("Enter User Name To Suspend:");

    try {
      realizeSuspendAccount(uName, password, ipAddress, uNameToSuspend);
    } catch (InvalidName | NotFound | CannotProceed
        | org.omg.CosNaming.NamingContextPackage.InvalidName e) {
      String err = "ERROR: CORBA services encountered an error";
      System.out.println(err);
      playerLog(err, uName, ipAddress);
    } catch (org.omg.CORBA.SystemException e) {
      handleServerDown(uName, ipAddress, e);
    } catch (UnknownServerRegionException e) {
      String err = "ERROR: Unknown Server for IP address!";
      System.out.println(err);
      adminLog(err, uName, "Unknown Server");
    }

  }

  private static void setFEORB() throws InvalidName, NotFound, CannotProceed,
      org.omg.CosNaming.NamingContextPackage.InvalidName {
    // create and initialize the ORB
    ORB orb = ORB.init(CLIENT_ORB_ARGS, null);
    String stringORB = "";

    try {
      BufferedReader bufferedReader =
          new BufferedReader(new FileReader("FRONT_END" + "ORBFile.txt"));
      stringORB = bufferedReader.readLine();
      bufferedReader.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    org.omg.CORBA.Object reference_CORBA = orb.string_to_object(stringORB);
    serverStub = GameServerHelper.narrow(reference_CORBA);
  }

  private static void realizeAdminSignIn(String uName, String password, String ipAddress)
      throws InvalidName, NotFound, CannotProceed,
      org.omg.CosNaming.NamingContextPackage.InvalidName, UnknownServerRegionException {
    String retStatement = serverStub.adminSignIn(uName, password, ipAddress);
    System.out.println(retStatement);
    adminLog(retStatement, uName, getRegionServer(ipAddress));
  }

  private static void realizeAdminSignOut(String uName, String ipAddress)
      throws InvalidName, NotFound, CannotProceed,
      org.omg.CosNaming.NamingContextPackage.InvalidName, UnknownServerRegionException {
    String retStatement = serverStub.adminSignOut(uName, ipAddress);
    System.out.println(retStatement);
    adminLog(retStatement, uName, getRegionServer(ipAddress));
  }

  private static void realizeAdminGetPlayerStatus(String uName, String password, String ipAddress)
      throws InvalidName, NotFound, CannotProceed,
      org.omg.CosNaming.NamingContextPackage.InvalidName, UnknownServerRegionException {
    String retStatement = serverStub.getPlayerStatus(uName, password, ipAddress);
    System.out.println(retStatement);
    adminLog(retStatement, uName, getRegionServer(ipAddress));
  }

  private static void realizeSuspendAccount(String uName, String password, String ipAddress,
      String uNameToSuspend) throws InvalidName, NotFound, CannotProceed,
      org.omg.CosNaming.NamingContextPackage.InvalidName, UnknownServerRegionException {
    String retStatement = serverStub.suspendAccount(uName, password, ipAddress, uNameToSuspend);
    System.out.println(retStatement);
    adminLog(retStatement, uName, getRegionServer(ipAddress));
  }

  private static void handleServerDown(String uName, String ipAddress, Exception e) {
    String err = "ERROR: Region server is not active";
    System.out.println(err);
    adminLog(err, uName, ipAddress);
  }


}
