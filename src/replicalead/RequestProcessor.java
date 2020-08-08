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
    PLAYER_CREATE_ACCOUNT, PLAYER_SIGN_IN, PLAYER_SIGN_OUT, PLAYER_TRANSFER_ACCOUNT, ADMIN_SIGN_IN, ADMIN_SIGN_OUT, ADMIN_GET_PLAYER_STATUS, ADMIN_SUSPEND_PLAYER_ACCOUNT, RESTART_REPLICA
  }

  private static final String MSG_SEP = "/";
  private static final String nullGameServerError = "ERR: GameServer does not exist!";
  private static final String insufficientParametersError =
      "ERR: Wrong number of parameters in this list!";

  private GameServer getServantORB(String ipAddress) {
    GameServer aGameServerRef = null;
    String args[] = null;

    try {
      if ("132".equals(ipAddress.substring(0, 3))) {
        ORB orb = ORB.init(args, null);

        BufferedReader br = new BufferedReader(new FileReader("ior_NorthAmerica.txt"));
        String ior = br.readLine();
        br.close();

        org.omg.CORBA.Object o = orb.string_to_object(ior);
        aGameServerRef = GameServerHelper.narrow(o);
      }

      else if ("93".equals(ipAddress.substring(0, 2))) {
        ORB orb = ORB.init(args, null);

        BufferedReader br = new BufferedReader(new FileReader("ior_Europe.txt"));
        String ior = br.readLine();
        br.close();

        org.omg.CORBA.Object o = orb.string_to_object(ior);
        aGameServerRef = GameServerHelper.narrow(o);
      }

      else if ("182".equals(ipAddress.substring(0, 3))) {
        ORB orb = ORB.init(args, null);

        BufferedReader br = new BufferedReader(new FileReader("ior_Asia.txt"));
        String ior = br.readLine();
        br.close();

        org.omg.CORBA.Object o = orb.string_to_object(ior);
        aGameServerRef = GameServerHelper.narrow(o);
      }

      else {
        System.out.println("ERR: GameServer does not exist for that location");
        aGameServerRef = null;
      }
    } catch (Exception e) {
      aGameServerRef = null;
      e.printStackTrace();
    }
    return aGameServerRef;
  }

  protected String performORBAction(String requestAction) throws InvalidName, ServantAlreadyActive,
      WrongPolicy, ObjectNotActive, FileNotFoundException, AdapterInactive {
    String requestParameterList[] = requestAction.split("/");

    sanitizeRequestParameterList(requestParameterList);

    if (requestParameterList != null) {
      int parameterListLength = requestParameterList.length;
      ACTION_TO_PERFORM actionToPerform = ACTION_TO_PERFORM.valueOf(requestParameterList[0]);
      return performRequestedAction(requestParameterList, parameterListLength, actionToPerform);
    }

    return "0";
  }

  private void sanitizeRequestParameterList(String[] requestParameterList) {
    for (int i = 0; i < requestParameterList.length; i++) {
      requestParameterList[i] = requestParameterList[i].trim();
    }
  }

  private String performRequestedAction(String[] requestParameterList, int parameterListLength,
      ACTION_TO_PERFORM actionToPerform) {
    switch (actionToPerform) {
      case PLAYER_CREATE_ACCOUNT: {
        if (parameterListLength == 7) {
          GameServer gameServerOrb = getServantORB(requestParameterList[5]);
          if (gameServerOrb == null) {
            System.out.println(nullGameServerError);
            return "0";
          }
          return gameServerOrb.createPlayerAccount(requestParameterList[1], requestParameterList[2],
              requestParameterList[3], requestParameterList[4], requestParameterList[5],
              Integer.parseInt(requestParameterList[6].trim()));
        } else {
          System.out.println(insufficientParametersError);
          return "0";
        }
      }

      case PLAYER_SIGN_IN: {
        if (parameterListLength == 4) {
          GameServer gameServerOrb = getServantORB(requestParameterList[3]);
          if (gameServerOrb == null) {
            System.out.println(nullGameServerError);
            return "0";
          }
          return gameServerOrb.playerSignIn(requestParameterList[1], requestParameterList[2],
              requestParameterList[3]);
        } else {
          System.out.println(insufficientParametersError);
          return "0";
        }
      }

      case PLAYER_SIGN_OUT: {
        if (parameterListLength == 3) {

          GameServer gameServerOrb = getServantORB(requestParameterList[2]);
          if (gameServerOrb == null) {
            System.out.println(nullGameServerError);
            return "0";
          }
          return gameServerOrb.playerSignOut(requestParameterList[1], requestParameterList[2]);
        } else {
          System.out.println(insufficientParametersError);
          return "0";
        }
      }

      case ADMIN_SIGN_IN: {
        if (parameterListLength == 4) {
          GameServer gameServerOrb = getServantORB(requestParameterList[3]);

          if (gameServerOrb == null) {
            System.out.println(nullGameServerError);
            return "0";
          }
          return gameServerOrb.adminSignIn(requestParameterList[1], requestParameterList[2],
              requestParameterList[3]);
        } else {
          System.out.println(insufficientParametersError);
          return "0";
        }
      }

      case ADMIN_SIGN_OUT: {
        if (parameterListLength == 3) {
          GameServer gameServerOrb = getServantORB(requestParameterList[2]);
          if (gameServerOrb == null) {
            System.out.println(nullGameServerError);
            return "0";
          }
          return gameServerOrb.adminSignOut(requestParameterList[1], requestParameterList[2]);
        } else {
          System.out.println(insufficientParametersError);
          return "0";
        }
      }

      case PLAYER_TRANSFER_ACCOUNT: {
        if (parameterListLength == 5) {

          GameServer gameServerOrb = getServantORB(requestParameterList[3]);
          if (gameServerOrb == null) {
            System.out.println(nullGameServerError);
            return "0";
          }
          return gameServerOrb.transferAccount(requestParameterList[1], requestParameterList[2],
              requestParameterList[3], requestParameterList[4]);
        } else {
          System.out.println(insufficientParametersError);
          return "0";
        }
      }

      case ADMIN_GET_PLAYER_STATUS: {
        if (parameterListLength == 4) {
          GameServer gameServerOrb = getServantORB(requestParameterList[3]);
          if (gameServerOrb == null) {
            System.out.println(nullGameServerError);
            return "0";
          }
          return gameServerOrb.getPlayerStatus(requestParameterList[1], requestParameterList[2],
              requestParameterList[3]);
        } else {
          System.out.println(insufficientParametersError);
          return "0";
        }
      }

      case ADMIN_SUSPEND_PLAYER_ACCOUNT: {
        if (parameterListLength == 5) {
          GameServer gameServerOrb = getServantORB(requestParameterList[3]);
          if (gameServerOrb == null) {
            System.out.println(nullGameServerError);
            return "0";
          }
          return gameServerOrb.suspendAccount(requestParameterList[1], requestParameterList[2],
              requestParameterList[3], requestParameterList[4]);
        } else {
          System.out.println(insufficientParametersError);
          return "0";
        }
      }

      default: {
        System.out.println("ERR: Unknown action requested");
        return "0";
      }
    }
  }

  protected void ProcessRMRequests(String requestFromReplicaManager) {
    String parameterList[] = requestFromReplicaManager.split(MSG_SEP);

    if (parameterList[0].substring(0, 15).equals("RESTART_REPLICA")) {
      System.out.println("Starting Replica LEAD...");
      GameServerNA naGameServer = new GameServerNA();
      GameServerEU euGameServer = new GameServerEU();
      GameServerAS asGameServer = new GameServerAS();
      naGameServer.start();
      euGameServer.start();
      asGameServer.start();
    }
  }
}
