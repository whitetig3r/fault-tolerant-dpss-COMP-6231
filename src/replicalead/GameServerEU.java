package replicalead;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

public class GameServerEU {
	public static void main(String[] args) {
		try{
			  final String[] defaultORBArgs = { "-ORBInitialPort", "1050" };
			  args = args.length == 0 ? defaultORBArgs : args;
		      ORB orb = ORB.init(args, null);
	
		      POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
		      rootpoa.the_POAManager().activate();
	
		      GameServerServant gameServer = new GameServerServant("EU");
		      gameServer.setORB(orb); 
	
		      org.omg.CORBA.Object ref = rootpoa.servant_to_reference(gameServer);
		      GameServer href = GameServerHelper.narrow(ref);
		          
		      // get the root naming context
		      org.omg.CORBA.Object objRef =
		          orb.resolve_initial_references("NameService");
		      NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
	
		      String name = "GameServerEU";
		      NameComponent path[] = ncRef.to_name( name );
		      ncRef.rebind(path, href);
	
		      System.out.println("GameServer EU ready and waiting ...");
	
		      orb.run();
		} catch (Exception e) {
		    System.err.println("ERROR: " + e);
		    e.printStackTrace(System.out);
		}

	}
}
