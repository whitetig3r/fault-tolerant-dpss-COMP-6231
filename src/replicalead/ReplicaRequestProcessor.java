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
		
		if(leaderResponse != null && replicaOneResponse != null  && replicaTwoResponse != null)
		{
			replicaResponsePendingCounter = 0;
	
			if(requestProcessed == true)
			{
				return;
			}
			
			System.out.println("Validating all responses are consistent...");
			
			String leaderResponseWithSep = leaderResponse + "/" + "$";
			String leaderResponseParts[] = leaderResponseWithSep.split(MSG_SEP);
			String replicaOneResponseParts[] = replicaOneResponse.split(MSG_SEP);
			String replicaTwoResponseParts[] = replicaTwoResponse.split(MSG_SEP);
					
			for(int i=0;i<leaderResponseParts.length;i++) {
				leaderResponseParts[i] = leaderResponseParts[i].trim(); 
			}
			
			for(int i=0;i<replicaOneResponseParts.length;i++) {
				replicaOneResponseParts[i] = replicaOneResponseParts[i].trim(); 
			}
			
			for(int i=0;i<replicaTwoResponseParts.length;i++) {
				replicaTwoResponseParts[i] = replicaTwoResponseParts[i].trim(); 
			}
			
			// check if all results are same
			if(leaderResponseParts[0].equals(replicaOneResponseParts[0]) || leaderResponseParts[0].equals(replicaTwoResponseParts[0]))
			{
				String inconsistenReplicaIdentifier = "";
				if(!leaderResponseParts[0].equals(replicaOneResponseParts[0]))
				{
					inconsistenReplicaIdentifier = "RA";
				}
			
				else if(!leaderResponseParts[0].equals(replicaTwoResponseParts[0]))
				{
					inconsistenReplicaIdentifier = "RB";
				}
				
				// Create a data packet for FE
				String requestDataFrontEnd = NAME_REPLICA_LEAD;
				String result = "";
				
				// sending packet to Leader
				for(int i = 0; i < leaderResponseParts.length; i++)
				{
					result  = result + MSG_SEP + leaderResponseParts[i];
				}
							
				requestDataFrontEnd = requestDataFrontEnd + result;
				
				
				// Sending datagram to Front End the result of Leader
				System.out.println("Sending datagram to the Front End... - " + requestDataFrontEnd);
				MainUDPThread.sendPacket(requestDataFrontEnd, FRONT_END_PORT);
								
				// sending packet to Replica Manager
				// If Replica Manager Datagram is not empty, send it.
				if(!inconsistenReplicaIdentifier.equals(""))
				{
					inconsistenReplicaIdentifier =  NAME_REPLICA_LEAD + MSG_SEP + inconsistenReplicaIdentifier;
					System.out.println("Sending Datagram to Replica Manager... - " + inconsistenReplicaIdentifier);
					MainUDPThread.sendPacket(inconsistenReplicaIdentifier, REPLICA_MANAGER_PORT);
				}
			
			}
			
			replicaOneResponse = null;
			replicaTwoResponse = null;
			leaderResponse = null;
		}
		else
		{			
			replicaResponsePendingCounter += 1;			
		}
		if(replicaResponsePendingCounter == 2)
		{
			String frontEndRequest = NAME_REPLICA_LEAD + MSG_SEP + leaderResponse;
			System.out.println("Sending datagram to Front End... - " + frontEndRequest);
			MainUDPThread.sendPacket(frontEndRequest, FRONT_END_PORT);
		}
	}
}
