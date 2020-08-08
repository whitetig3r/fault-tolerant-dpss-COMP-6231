package replicalead;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

public class GameServerAS extends Thread {
  public void run() {
    try {
      final String[] defaultORBArgs = {"-ORBInitialPort", "1050"};
      ORB orb = ORB.init(defaultORBArgs, null);

      POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
      rootpoa.the_POAManager().activate();

      GameServerServant gameServer =
          new GameServerServant("AS", new ArrayList<>(Arrays.asList(8989, 8990)), 8991);
      gameServer.setORB(orb);

      org.omg.CORBA.Object ref = rootpoa.servant_to_reference(gameServer);

      String ior = orb.object_to_string(ref);

      PrintWriter file = new PrintWriter("ior_Asia.txt");
      file.println(ior);
      file.close();

      GameServer href = GameServerHelper.narrow(ref);

      // get the root naming context
      org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
      NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

      String name = "GameServerAS";
      NameComponent path[] = ncRef.to_name(name);
      ncRef.rebind(path, href);

      System.out.println("INFO -- GameServer AS is up");

      orb.run();
    } catch (Exception e) {
      System.err.println("ERR: " + e);
      e.printStackTrace();
    }

  }
}
