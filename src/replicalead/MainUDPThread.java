package replicalead;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;


public class MainUDPThread extends Thread {
  private static final int REPLICA_LEAD_PORT = 4321;
  private static final int REPLICA_LEAD_MULTICAST_PORT = 10105;
  private static final String REPLICA_COMMUNICATION_MULTICAST_ADDR = "225.1.1.10";
  private static final String MSG_SEP = "%";
  private static final int BUFFER_SIZE = 1200;
  private static final String NAME_REPLICA_LEAD = "REPLICA_LEADER";
  private String extractedDatagram;
  public static CopyOnWriteArrayList<String> receivedDatagrams = new CopyOnWriteArrayList<String>();

  public static void main(String[] args) {
    MainUDPThread replicaLeaderThread = new MainUDPThread();
    replicaLeaderThread.start();
  }

  @Override
  public void run() {
    try {
      listenForRequests();
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Server thread interrupted.");
    }
  }

  MainUDPThread() {}

  protected void listenForRequests() throws InvalidName, ServantAlreadyActive, WrongPolicy,
      ObjectNotActive, AdapterInactive, InterruptedException {
    DatagramSocket aSocket = null;
    try {
      aSocket = new DatagramSocket(REPLICA_LEAD_PORT);
      System.out.println("REPLICA LEADER -- UDP Thread is going online!");
      while (true) {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket request = new DatagramPacket(buffer, buffer.length);
        aSocket.receive(request);
        String requestData = new String(request.getData(), "UTF-8");

        if (requestData != null) {
          String requestSender = extractSender(requestData);
          if (requestSender != null) {
            handleRequest(requestSender);
          }
        }
      }
    } catch (SocketException e) {
      System.out.println("Socket: " + e.getMessage());
    } catch (IOException e) {
      System.out.println("IO: " + e.getMessage());
    } finally {
      if (aSocket != null)
        aSocket.close();
    }
  }

  private void handleRequest(String requestSender)
      throws InvalidName, ServantAlreadyActive, WrongPolicy, ObjectNotActive, FileNotFoundException,
      AdapterInactive, IOException, InterruptedException {
    switch (requestSender) {
      case "FRONT_END":
        System.out.println("Receiving Datagram from Front End...");
        RequestProcessor requestProcessorFE = new RequestProcessor();
        ReplicaRequestProcessor.requestProcessed = false;
        if (extractedDatagram != "") {
          String multicastDatagramData = NAME_REPLICA_LEAD + MSG_SEP + extractedDatagram;
          receivedDatagrams.add(extractedDatagram);
          System.out.println("Datagram Data sent to Front End - " + multicastDatagramData);
          ReplicaRequestProcessor.leaderResponse =
              requestProcessorFE.performORBAction(extractedDatagram);
          sendMulticastToReplicaGroups(multicastDatagramData);
        }
        requestProcessorFE = null;
        extractedDatagram = "";
        break;

      case "REPLICA_MANAGER":
        System.out.println("Receiving Datagram from Replica Manager... - " + extractedDatagram);
        RequestProcessor requestProcessorRM = new RequestProcessor();
        requestProcessorRM.ProcessRMRequests(extractedDatagram);
        extractedDatagram = "";
        break;

      case "REPLICA_ONE":
        System.out.println("Receiving Datagram from Replica 1... - " + extractedDatagram);

        if (extractedDatagram != "") {
          ReplicaRequestProcessor.replicaOneResponse = extractedDatagram;
          ReplicaRequestProcessor.verifyConsistentResults();
        }

        extractedDatagram = "";
        break;

      case "REPLICA_TWO":
        System.out.println("Receiving Datagram from Replica 2... - " + extractedDatagram);
        if (extractedDatagram != "") {
          ReplicaRequestProcessor.replicaTwoResponse = extractedDatagram;
          ReplicaRequestProcessor.verifyConsistentResults();
        }

        extractedDatagram = "";
        break;

      default:
        System.out.println("ERR: Sender is UNKNOWN.");
        break;
    }
  }

  public static boolean sendPacket(String requestData, int port) {
    DatagramSocket aSocket = null;
    try {
      aSocket = new DatagramSocket();
      byte[] m = requestData.getBytes();
      InetAddress aHost = InetAddress.getByName("localhost");
      int serverPort = port;
      DatagramPacket request = new DatagramPacket(m, requestData.length(), aHost, serverPort);
      aSocket.send(request);
      return true;
    } catch (SocketException e) {
      System.out.println("Socket: " + e.getMessage());
    } catch (IOException e) {
      System.out.println("IO: " + e.getMessage());
    } finally {
      if (aSocket != null)
        aSocket.close();
    }

    return false;
  }

  protected static boolean sendMulticastToReplicaGroups(String requestData)
      throws IOException, InterruptedException {
    DatagramSocket socket = null;

    try {
      socket = new DatagramSocket();

      byte[] buffer = requestData.getBytes();
      DatagramPacket dataGram;

      System.out.println("Dispatching multicast request to replica groups -- " + requestData);

      dataGram = new DatagramPacket(buffer, buffer.length,
          InetAddress.getByName(REPLICA_COMMUNICATION_MULTICAST_ADDR), REPLICA_LEAD_MULTICAST_PORT);
      socket.send(dataGram);
    }

    catch (SocketException e) {
      System.out.println("Socket: " + e.getMessage());
    } catch (IOException e) {
      System.out.println("IO: " + e.getMessage());
    } finally {
      if (socket != null)
        socket.close();
    }

    return false;
  }

  protected String extractSender(String responseData) {
    String extractedParts[] = responseData.split(MSG_SEP);
    if (extractedParts != null) {
      extractedDatagram =
          Arrays.stream(Arrays.copyOfRange(extractedParts, 1, extractedParts.length))
              .collect(Collectors.joining(MSG_SEP));
      return extractedParts[0];
    }
    System.out.println("ERR: Failed to extract sender information");
    return null;
  }

}
