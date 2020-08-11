package frontend;

import java.util.concurrent.ConcurrentHashMap;
import org.omg.CORBA.ORB;

public class GameServerORBThread extends Thread {
  private ORB orb;
  private static boolean receivedResponse;
  private static String confirmed;
  private static String leadResponse;

  public static ConcurrentHashMap<String, String> responseHash = new ConcurrentHashMap<>();

  protected GameServerORBThread(ORB orb) {
    this.orb = orb;
    receivedResponse = false;
    confirmed = null;
  }

  protected static void setLeaderResponded(boolean responded) {
    receivedResponse = responded;
  }

  protected static boolean hasLeaderResponded() {
    return receivedResponse;
  }

  protected static void setConfimation(String confirmation) {
    confirmed = confirmation;
  }


  protected static String getConfirmation() {
    return confirmed;
  }

  protected static void setResponse(String response) {
    leadResponse = response;
  }


  protected static String getResponse() {
    return leadResponse;
  }


  public void run() {
    orb.run();
  }
}
