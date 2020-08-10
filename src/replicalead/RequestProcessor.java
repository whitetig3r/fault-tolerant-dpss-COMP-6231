package replicalead;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

public class RequestProcessor {

  private static enum ACTION_TO_PERFORM {
    PLAYER_CREATE_ACCOUNT, PLAYER_SIGN_IN, PLAYER_SIGN_OUT, PLAYER_TRANSFER_ACCOUNT, ADMIN_SIGN_IN, ADMIN_SIGN_OUT, ADMIN_GET_PLAYER_STATUS, ADMIN_SUSPEND_PLAYER_ACCOUNT, REPLACE_REPLICA
  }

  private static final String MSG_SEP = "%";
  private static final String nullGameServerError = "ERR: GameServer does not exist!";
  private static final String insufficientParametersError =
      "ERR: Wrong number of parameters in this list!";

  public GameServer getServantORB(String ipAddress) {
    GameServer gameServerRef = null;
    String args[] = null;
    ORB orb = ORB.init(args, null);
    try {
      switch (ipAddress.substring(0, 3)) {
        case "132": {
          BufferedReader br = new BufferedReader(new FileReader("NAORBReplicaLead.txt"));
          String orbString = br.readLine();
          br.close();
          org.omg.CORBA.Object orbObject = orb.string_to_object(orbString);
          gameServerRef = GameServerHelper.narrow(orbObject);
          break;
        }
        case "93.": {
          BufferedReader br = new BufferedReader(new FileReader("EUORBReplicaLead.txt"));
          String orbString = br.readLine();
          br.close();
          org.omg.CORBA.Object orbObject = orb.string_to_object(orbString);
          gameServerRef = GameServerHelper.narrow(orbObject);
          break;
        }
        case "182": {
          BufferedReader br = new BufferedReader(new FileReader("ASORBReplicaLead.txt"));
          String orbString = br.readLine();
          br.close();
          org.omg.CORBA.Object orbObject = orb.string_to_object(orbString);
          gameServerRef = GameServerHelper.narrow(orbObject);
          break;
        }
        default: {
          System.out.println("ERR: GameServer does not exist for that location");
          gameServerRef = null;
        }
      }
    } catch (Exception e) {
      gameServerRef = null;
      e.printStackTrace();
    }
    return gameServerRef;
  }

  protected String performORBAction(String requestAction) throws InvalidName, ServantAlreadyActive,
      WrongPolicy, ObjectNotActive, FileNotFoundException, AdapterInactive {
    String requestParameterList[] = requestAction.split("%");

    sanitizeRequestParameterList(requestParameterList);

    if (requestParameterList != null) {
      int parameterListLength = requestParameterList.length;
      ACTION_TO_PERFORM actionToPerform = ACTION_TO_PERFORM.valueOf(requestParameterList[0]);
      return performRequestedAction(requestParameterList, parameterListLength, actionToPerform);
    }
    return "ERR";
  }

  private void sanitizeRequestParameterList(String[] requestParameterList) {
    for (int i = 0; i < requestParameterList.length; i++) {
      requestParameterList[i] = requestParameterList[i].trim();
    }
  }

  private String performRequestedAction(String[] requestParameterList, int parameterListLength,
      ACTION_TO_PERFORM actionToPerform) {
    switch (actionToPerform) {
      case PLAYER_CREATE_ACCOUNT:
        return processAction(requestParameterList, 7, requestParameterList[5],
            ACTION_TO_PERFORM.PLAYER_CREATE_ACCOUNT);

      case PLAYER_SIGN_IN:
        return processAction(requestParameterList, 4, requestParameterList[3],
            ACTION_TO_PERFORM.PLAYER_SIGN_IN);

      case PLAYER_SIGN_OUT:
        return processAction(requestParameterList, 3, requestParameterList[2],
            ACTION_TO_PERFORM.PLAYER_SIGN_OUT);

      case ADMIN_SIGN_IN:
        return processAction(requestParameterList, 3, requestParameterList[2],
            ACTION_TO_PERFORM.ADMIN_SIGN_IN);

      case ADMIN_SIGN_OUT:
        return processAction(requestParameterList, 3, requestParameterList[2],
            ACTION_TO_PERFORM.ADMIN_SIGN_OUT);

      case PLAYER_TRANSFER_ACCOUNT:
        return processAction(requestParameterList, 5, requestParameterList[3],
            ACTION_TO_PERFORM.PLAYER_TRANSFER_ACCOUNT);

      case ADMIN_GET_PLAYER_STATUS:
        return processAction(requestParameterList, 4, requestParameterList[3],
            ACTION_TO_PERFORM.ADMIN_GET_PLAYER_STATUS);

      case ADMIN_SUSPEND_PLAYER_ACCOUNT:
        return processAction(requestParameterList, 5, requestParameterList[3],
            ACTION_TO_PERFORM.ADMIN_SUSPEND_PLAYER_ACCOUNT);

      default:
        System.out.println("ERR: Unknown action requested");
        return "ERR";
    }
  }

  protected void processRequestsFromReplicaManager(String requestFromReplicaManager) {
    String parameterList[] = requestFromReplicaManager.split(MSG_SEP);

    if (parameterList[0].substring(0, 15).equals("REPLACE_REPLICA")) {
      System.out.println("Starting Replica LEAD...");
      GameServerNA naGameServer = new GameServerNA();
      GameServerEU euGameServer = new GameServerEU();
      GameServerAS asGameServer = new GameServerAS();
      naGameServer.start();
      euGameServer.start();
      asGameServer.start();
    }
  }

  protected String processAction(String[] requestParameterList, int requiredLength,
      String ipAddress, ACTION_TO_PERFORM action) {
    if (requestParameterList.length == requiredLength) {
      GameServer gameServerOrb = getServantORB(ipAddress);
      if (gameServerOrb == null) {
        System.out.println(nullGameServerError);
        return "ERR";
      }
      switch (action) {
        case PLAYER_CREATE_ACCOUNT:
          return gameServerOrb.createPlayerAccount(requestParameterList[1], requestParameterList[2],
              requestParameterList[3], requestParameterList[4], requestParameterList[5],
              Integer.parseInt(requestParameterList[6]));
        case PLAYER_SIGN_IN:
          return gameServerOrb.playerSignIn(requestParameterList[1], requestParameterList[2],
              requestParameterList[3]);
        case PLAYER_SIGN_OUT:
          return gameServerOrb.playerSignOut(requestParameterList[1], requestParameterList[2]);
        case ADMIN_SIGN_IN:
          return gameServerOrb.adminSignIn(requestParameterList[1], requestParameterList[2],
              requestParameterList[3]);
        case ADMIN_SIGN_OUT:
          return gameServerOrb.adminSignOut(requestParameterList[1], requestParameterList[2]);
        case PLAYER_TRANSFER_ACCOUNT:
          return gameServerOrb.transferAccount(requestParameterList[1], requestParameterList[2],
              requestParameterList[3], requestParameterList[4]);
        case ADMIN_GET_PLAYER_STATUS:
          return gameServerOrb.getPlayerStatus(requestParameterList[1], requestParameterList[2],
              requestParameterList[3]);
        case ADMIN_SUSPEND_PLAYER_ACCOUNT:
          return gameServerOrb.suspendAccount(requestParameterList[1], requestParameterList[2],
              requestParameterList[3], requestParameterList[4]);
        default:
          return "BAD_OP"; // never should return this
      }
    } else {
      System.out.println(insufficientParametersError);
      return "ERR";
    }
  }
}
