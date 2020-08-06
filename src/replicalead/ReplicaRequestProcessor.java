package replicalead;

public class ReplicaRequestProcessor {

	static String m_LeaderResultProcessed;
	static String m_Replica_A_Processed;
	static String m_Replica_B_Processed;
	static boolean m_HasBeenProcessed;
	
	private static int m_checkedByPrevReplica;
	
	private static final String UDP_PARSER = "/";
	
	private static final String LR_NAME = "LR";
	private static final int UDP_PORT_FE = 6000;
	private static final int UDP_PORT_REPLICA_MANAGER = 5000;
	
	protected static void CompareResults() {
		System.out.println("Result Processed By Leader - " + m_LeaderResultProcessed);
		System.out.println("Result Processed By Replica A - " + m_Replica_A_Processed);
		System.out.println("Result Processed By Replica B - " + m_Replica_B_Processed);
		
		if(m_LeaderResultProcessed != null && m_Replica_A_Processed != null  && m_Replica_B_Processed != null)
		{
			m_checkedByPrevReplica = 0;
	
			if(m_HasBeenProcessed == true)
			{
				return;
			}
			
			System.out.println("All # Results are Valid (comparision Underway)");
			
			String l_leaderData_end_parser = m_LeaderResultProcessed + "/" + "$";
			String l_segments_Leader[] = l_leaderData_end_parser.split(UDP_PARSER);
			String l_segments_A[] = m_Replica_A_Processed.split(UDP_PARSER);
			String l_segments_B[] = m_Replica_B_Processed.split(UDP_PARSER);
					
			for(int i=0;i<l_segments_Leader.length;i++) {
				l_segments_Leader[i] = l_segments_Leader[i].trim(); 
			}
			
			for(int i=0;i<l_segments_A.length;i++) {
				l_segments_A[i] = l_segments_A[i].trim(); 
			}
			
			for(int i=0;i<l_segments_B.length;i++) {
				l_segments_B[i] = l_segments_B[i].trim(); 
			}
			
			// check if all results are same
			if(l_segments_Leader[0].equals(l_segments_A[0]) || l_segments_Leader[0].equals(l_segments_B[0]))
			{
				String l_rmdatagram = "";
				if(!l_segments_Leader[0].equals(l_segments_A[0]))
				{
					l_rmdatagram = "RA";
				}
			
				else if(!l_segments_Leader[0].equals(l_segments_B[0]))
				{
					l_rmdatagram = "RB";
				}
				
				// Create a data packet for FE
				String l_Data_FE = LR_NAME;
				String result = "";
				
				// sending packet to Leader
				for(int i = 0; i < l_segments_Leader.length; i++)
				{
					result  = result + UDP_PARSER + l_segments_Leader[i];
				}
							
				l_Data_FE = l_Data_FE + result;
				
				
				// Sending datagram to Front End the result of Leader
				System.out.println("LocalReplicsRequestProcessing.CompareResults: to Front End - l_Data: " + l_Data_FE);
				MainUDPThread.sendPacket(l_Data_FE, UDP_PORT_FE);
								
				// sending packet to Replica Manager
				// If Replica Manager Datagram is not empty, send it.
				if(!l_rmdatagram.equals(""))
				{
					l_rmdatagram =  LR_NAME + UDP_PARSER + l_rmdatagram;
					System.out.println("Data gram sent to replica manager l_rmdatagram - " + l_rmdatagram);
					MainUDPThread.sendPacket(l_rmdatagram, UDP_PORT_REPLICA_MANAGER);
				}
			
			}
			
			m_Replica_A_Processed = null;
			m_Replica_B_Processed = null;
			m_LeaderResultProcessed = null;
		}
		// If LR RA and RB have not yet returned a value
		else
		{			
			m_checkedByPrevReplica += 1;
			System.out.println("Result tried to be Processed By a Replica - " + m_checkedByPrevReplica);
		}
		
		// Increment the check
		// if check reaches 2, send data to FE
		if(m_checkedByPrevReplica == 2)
		{
			//m_LeaderResultProcessed = LR_NAME + UDP_PARSER + m_LeaderResultProcessed;
			String FEDAta = LR_NAME + UDP_PARSER + m_LeaderResultProcessed;
			System.out.println("LocalReplicsRequestProcessing.CompareResults: to Front End - FEDAta: " + FEDAta);
			MainUDPThread.sendPacket(FEDAta, UDP_PORT_FE);
		}
	}
}
