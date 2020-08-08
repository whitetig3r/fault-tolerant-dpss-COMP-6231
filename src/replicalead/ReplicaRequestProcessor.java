package replicalead;

public class ReplicaRequestProcessor {

  static String leaderResponse;
  static String replicaOneResponse;
  static String replicaTwoResponse;
  static boolean requestProcessed;
  private static int replicaResponsePendingCounter;
  private static final String MSG_SEP = "/";
  private static final String NAME_REPLICA_LEAD = "LR";
  private static final int FRONT_END_PORT = 6000;
  private static final int REPLICA_MANAGER_PORT = 5000;

  protected static void verifyConsistentResults() {
    if (leaderResponse != null && replicaOneResponse != null && replicaTwoResponse != null) {
      replicaResponsePendingCounter = 0;

      if (requestProcessed == true) {
        return;
      }

      System.out.println("Validating all responses are consistent...");
      String leaderResponseWithSep = leaderResponse + "/" + "$";
      String leaderResponseParts[] = leaderResponseWithSep.split(MSG_SEP);
      String replicaOneResponseParts[] = replicaOneResponse.split(MSG_SEP);
      String replicaTwoResponseParts[] = replicaTwoResponse.split(MSG_SEP);
      sanitizeResponse(leaderResponseParts);
      sanitizeResponse(replicaOneResponseParts);
      sanitizeResponse(replicaTwoResponseParts);

      if (leaderResponseParts[0].equals(replicaOneResponseParts[0])
          || leaderResponseParts[0].equals(replicaTwoResponseParts[0])) {
        handleCompletedResponses(leaderResponseParts, replicaOneResponseParts,
            replicaTwoResponseParts);

      }
      replicaOneResponse = null;
      replicaTwoResponse = null;
      leaderResponse = null;
    } else {
      replicaResponsePendingCounter += 1;
    }
    if (replicaResponsePendingCounter == 2) {
      String frontEndRequest = NAME_REPLICA_LEAD + MSG_SEP + leaderResponse;
      System.out.println("Sending datagram to Front End... - " + frontEndRequest);
      MainUDPThread.sendPacket(frontEndRequest, FRONT_END_PORT);
    }
  }

  private static void handleCompletedResponses(String[] leaderResponseParts,
      String[] replicaOneResponseParts, String[] replicaTwoResponseParts) {
    String inconsistenReplicaIdentifier = "";
    if (!leaderResponseParts[0].equals(replicaOneResponseParts[0])) {
      inconsistenReplicaIdentifier = "RA";
    }

    else if (!leaderResponseParts[0].equals(replicaTwoResponseParts[0])) {
      inconsistenReplicaIdentifier = "RB";
    }
    String requestDataFrontEnd = NAME_REPLICA_LEAD;
    String result = "";
    for (int i = 0; i < leaderResponseParts.length; i++) {
      result = result + MSG_SEP + leaderResponseParts[i];
    }
    requestDataFrontEnd = requestDataFrontEnd + result;
    System.out.println("Sending datagram to the Front End... - " + requestDataFrontEnd);
    MainUDPThread.sendPacket(requestDataFrontEnd, FRONT_END_PORT);
    if (!inconsistenReplicaIdentifier.equals("")) {
      inconsistenReplicaIdentifier = NAME_REPLICA_LEAD + MSG_SEP + inconsistenReplicaIdentifier;
      System.out
          .println("Sending Datagram to Replica Manager... - " + inconsistenReplicaIdentifier);
      MainUDPThread.sendPacket(inconsistenReplicaIdentifier, REPLICA_MANAGER_PORT);
    }
  }

  private static void sanitizeResponse(String[] leaderResponseParts) {
    for (int i = 0; i < leaderResponseParts.length; i++) {
      leaderResponseParts[i] = leaderResponseParts[i].trim();
    }
  }
}
