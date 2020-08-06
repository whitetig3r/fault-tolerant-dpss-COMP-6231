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
		  PLAYER_CREATE_ACCOUNT,
		  PLAYER_SIGN_IN,
		  PLAYER_SIGN_OUT,
		  PLAYER_TRANSFER_ACCOUNT,
		  ADMIN_SIGN_IN,
		  ADMIN_SIGN_OUT,
		  ADMIN_GET_PLAYER_STATUS, 
		  ADMIN_SUSPEND_PLAYER_ACCOUNT, 
		  RESTART_REPLICA
	}
	
	private static final String UDP_PARSER = "/";

	// extract ip address, get required Local Server <location> reference
		private GameServer  getServerReference(String p_IPAddress)
		{
			GameServer aGameServerRef = null;
			String args[] = null;
			
			try
			{
				if("132".equals(p_IPAddress.substring(0,3)))
				{
					ORB orb = ORB.init(args, null);
					
					BufferedReader br = new BufferedReader(new FileReader("ior_NorthAmerica.txt"));
					String ior = br.readLine();
					br.close();
			
					org.omg.CORBA.Object o = orb.string_to_object(ior);
					aGameServerRef = GameServerHelper.narrow(o);			
				}
			
				else if("93".equals(p_IPAddress.substring(0,2)))
				{
					ORB orb = ORB.init(args, null);
					
					BufferedReader br = new BufferedReader(new FileReader("ior_Europe.txt"));
					String ior = br.readLine();
					br.close();
			
					org.omg.CORBA.Object o = orb.string_to_object(ior);
					aGameServerRef = GameServerHelper.narrow(o);	
				}
			
				else if("182".equals(p_IPAddress.substring(0,3)))
				{
					ORB orb = ORB.init(args, null);
					
					BufferedReader br = new BufferedReader(new FileReader("ior_Asia.txt"));
					String ior = br.readLine();
					br.close();
			
					org.omg.CORBA.Object o = orb.string_to_object(ior);
					aGameServerRef = GameServerHelper.narrow(o);	
				}
			
				else
				{
					System.out.println("LocalOrbProcessing.getServerReference : Error - IP Location Index not Valid/n");
					aGameServerRef =  null;
				}
			}
			catch(Exception e)
			{
				aGameServerRef = null;
				e.printStackTrace();					
			}
			return aGameServerRef;
		}
		
		protected String performRMI(String p_input) throws InvalidName, ServantAlreadyActive, WrongPolicy, ObjectNotActive, FileNotFoundException, AdapterInactive
		{
			String l_ParamArray[] = p_input.split("/");
			
			for(int i=0; i<l_ParamArray.length; i++) {
				l_ParamArray[i] = l_ParamArray[i].trim();
			}
			
			if(l_ParamArray != null)
			{
				
				int l_numElements = l_ParamArray.length;
				
				ACTION_TO_PERFORM l_functionValue = ACTION_TO_PERFORM.valueOf(l_ParamArray[0]);
				
				// Send CREATE PLAYER ACCOUNT
				if(l_functionValue == ACTION_TO_PERFORM.PLAYER_CREATE_ACCOUNT) 
				{
					System.out.println("LocalOrbProcessing.performRMI : Creating Player Account Request/n");
					if(l_numElements == 7)
					{
						
						GameServer l_LocalGameServerReference =  getServerReference(l_ParamArray[5]);
						
						if(l_LocalGameServerReference == null)
						{
							System.out.println("LocalOrbProcessing.performRMI : Error - Cannot perform RMI, GameServer = NULL/n");
							return "0";
						}
						
						return l_LocalGameServerReference.createPlayerAccount(l_ParamArray[1], l_ParamArray[2], l_ParamArray[3], l_ParamArray[4], l_ParamArray[5], Integer.parseInt(l_ParamArray[6].trim()));
					}
					else
					{
						System.out.println("LocalOrbProcessing.performRMI : Error: Have not parsed enough params for create player account/n");
						return "0";
					}
				}
				
				// Send PLAYER SIGN IN
				// playerSignIn(String UserName, String Password, String IPAddres);
				else if(l_functionValue == ACTION_TO_PERFORM.PLAYER_SIGN_IN) 
				{
					System.out.println("LocalOrbProcessing.performRMI : Player Sign In Request/n");
					if(l_numElements == 4)
					{

						GameServer l_LocalGameServerReference =  getServerReference(l_ParamArray[3]);
						
						if(l_LocalGameServerReference == null)
						{
							System.out.println("LocalOrbProcessing.performRMI : Error - Cannot perform RMI, GameServer = NULL/n");
							return "0";
						}
						
						return l_LocalGameServerReference.playerSignIn(l_ParamArray[1], l_ParamArray[2], l_ParamArray[3]);
					}
					else
					{
						System.out.println("LocalOrbProcessing.performRMI : Error: Have not parsed enough params for player sign in/n");
						return "0";
					}
				}
				
				// Send PLAYER SIGN OUT
				// playerSignOut(String p_Username, String IPAddress) 
				else if(l_functionValue == ACTION_TO_PERFORM.PLAYER_SIGN_OUT) 
				{
					System.out.println("LocalOrbProcessing.performRMI : Player Sign Out Request/n");
					if(l_numElements == 3)
					{

						GameServer l_LocalGameServerReference =  getServerReference(l_ParamArray[2]);
						
						if(l_LocalGameServerReference == null)
						{
							System.out.println("LocalOrbProcessing.performRMI : Error - Cannot perform RMI, GameServer = NULL/n");
							return "0";
						}
						
						return l_LocalGameServerReference.playerSignOut(l_ParamArray[1], l_ParamArray[2]);
					}
					else
					{
						System.out.println("LocalOrbProcessing.performRMI : Error: Have not parsed enough params for player sign out/n");
						return "0";
					}
				}
				
				else if(l_functionValue == ACTION_TO_PERFORM.ADMIN_SIGN_IN) 
				{
					System.out.println("LocalOrbProcessing.performRMI : Admin Sign In Request/n");
					if(l_numElements == 4)
					{

						GameServer l_LocalGameServerReference =  getServerReference(l_ParamArray[3]);
						
						if(l_LocalGameServerReference == null)
						{
							System.out.println("LocalOrbProcessing.performRMI : Error - Cannot perform RMI, GameServer = NULL/n");
							return "0";
						}
						return l_LocalGameServerReference.adminSignIn(l_ParamArray[1], l_ParamArray[2], l_ParamArray[3]);
					}
					else
					{
						System.out.println("LocalOrbProcessing.performRMI : Error: Have not parsed enough params for admin sign in/n");
						return "0";
					}
				}
				
				// Send ADMIN SIGN OUT
				// adminSignOut(String p_Username, String IPAddress) 
				else if(l_functionValue == ACTION_TO_PERFORM.ADMIN_SIGN_OUT) 
				{
					System.out.println("LocalOrbProcessing.performRMI : Admin Sign Out Request/n");
					if(l_numElements == 3)
					{

						GameServer l_LocalGameServerReference =  getServerReference(l_ParamArray[2]);
						
						if(l_LocalGameServerReference == null)
						{
							System.out.println("LocalOrbProcessing.performRMI : Error - Cannot perform RMI, GameServer = NULL/n");
							return "0";
						}
						
						return l_LocalGameServerReference.adminSignOut(l_ParamArray[1], l_ParamArray[2]);

					}
					else
					{
						System.out.println("LocalOrbProcessing.performRMI : Error: Have not parsed enough params for admin sign out/n");
						return "0";
					}
				}
				
				// Send PLAYER TRANSFER ACCOUNT
				//String transferAccount(String p_Username, String p_Password, String p_oldIPAddress, String p_newIPAddress)
				else if(l_functionValue == ACTION_TO_PERFORM.PLAYER_TRANSFER_ACCOUNT) 
				{
					System.out.println("LocalOrbProcessing.performRMI : Player Account Transfer Request/n");
					if(l_numElements == 5)
					{

						GameServer l_LocalGameServerReference =  getServerReference(l_ParamArray[3]);
						
						if(l_LocalGameServerReference == null)
						{
							System.out.println("LocalOrbProcessing.performRMI : Error - Cannot perform RMI, GameServer = NULL/n");
							return "0";
						}
						
						return l_LocalGameServerReference.transferAccount(l_ParamArray[1], l_ParamArray[2], l_ParamArray[3], l_ParamArray[4]);
					}
					else
					{
						System.out.println("LocalOrbProcessing.performRMI : Error: Have not parsed enough params for player account transfer/n");
						return "0";
					}
				}
				
				// Send GET PLAYER STATUS
				// getPlayerStatus(String AdminUserName, String AdminPassword, String AdminIPAddress);
				else if(l_functionValue ==  ACTION_TO_PERFORM.ADMIN_GET_PLAYER_STATUS) 
				{
					System.out.println("LocalOrbProcessing.performRMI : Get Player Status Request/n");
					if(l_numElements == 4)
					{

						GameServer l_LocalGameServerReference =  getServerReference(l_ParamArray[3]);
						
						if(l_LocalGameServerReference == null)
						{
							System.out.println("LocalOrbProcessing.performRMI : Error - Cannot perform RMI, GameServer = NULL/n");
							return "0";
						}
						
						return l_LocalGameServerReference.getPlayerStatus(l_ParamArray[1], l_ParamArray[2], l_ParamArray[3]);
					}
					else
					{
						System.out.println("LocalOrbProcessing.performRMI : Error: Have not parsed enough params for get player status/n");
						return "0";
					}
				}
				
				// Send SUSPEND ACCOUNT
				// suspendAccount(String p_AdminUserName, String p_AdminPassword, String p_AdminIPAddress, String p_UsernametoSuspend) 
				else if(l_functionValue == ACTION_TO_PERFORM.ADMIN_SUSPEND_PLAYER_ACCOUNT) 
				{
					System.out.println("LocalOrbProcessing.performRMI : Get Suspend Account Request/n");
					if(l_numElements == 5)
					{

						GameServer l_LocalGameServerReference =  getServerReference(l_ParamArray[3]);
						
						if(l_LocalGameServerReference == null)
						{
							System.out.println("LocalOrbProcessing.performRMI : Error - Cannot perform RMI, GameServer = NULL/n");
							return "0";
						}
						
						return l_LocalGameServerReference.suspendAccount(l_ParamArray[1], l_ParamArray[2], l_ParamArray[3], l_ParamArray[4]);
					}
					else
					{
						System.out.println("LocalOrbProcessing.performRMI : Error: Have not parsed enough params for suspend account/n");
						return "0";
					}
				} 
			}
			System.out.println("LocalOrbProcessing.performRMI : Error: Parsing for method selection not done right");
			return "0";
			
		}
		
		protected void ProcessRMRequests(String p_input)
		{
			String l_ParamArray[] = p_input.split(UDP_PARSER);
				
			System.out.println("LocalRMRequestProcessing.getMethodName: l_ParamArray[0].substring(0, 15) - " + l_ParamArray[0].substring(0, 15));
			
			if(l_ParamArray[0].substring(0, 15).equals("RESTART_REPLICA"))
			{
				System.out.println("Starting Replica LEAD");
				// Creating Instances of Local Servers - North America, Europe and Asia
				GameServerNA l_GameServer_NorthAmerica = new GameServerNA();
				GameServerEU l_GameServer_Europe = new GameServerEU();
				GameServerAS l_GameServer_Asia = new GameServerAS();
				l_GameServer_NorthAmerica.start();
				l_GameServer_Europe.start();
				l_GameServer_Asia.start();
			}
		}
}
