package frontend;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class GameServerFEThread extends Thread {
  private DatagramSocket frontEndSocket;
  private int port;
  private boolean threadDown;

  protected GameServerFEThread(int port) {
    this.port = port;
    threadDown = false;
    try {
      frontEndSocket = new DatagramSocket(port);
    } catch (SocketException e) {
      threadDown = true;
    }
  }

  protected boolean hasCrashed() {
    return threadDown;
  }

  @Override
  public void run() {
    while (true) {
      try {
        synchronized (this) {
          byte[] buffer = new byte[1200];
          DatagramPacket request = new DatagramPacket(buffer, buffer.length);
          frontEndSocket.receive(request);
          String[] parameterList = (new String(request.getData())).split("%");
          if (parameterList[0].equals("REPLICA_LEADER")) {
            GameServerORBThread.responseHash.put(parameterList[parameterList.length - 2],
                parameterList[1]);
            GameServerORBThread.setConfimation(parameterList[1]);
            GameServerORBThread.setLeaderResponded(true);
          }
        }
      } catch (IOException e) {
        frontEndSocket.close();
        threadDown = true;
      }
    }
  }
}
