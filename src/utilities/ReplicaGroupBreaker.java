package utilities;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReplicaGroupBreaker {
  private static final int UDP_BREAKER_PORT = 9393;

  public static void main(String[] args) {
    runRegionUdpServer();
    Scanner keyb = new Scanner(System.in);
    int port = -1;
    System.out.println(
        "Pick \"1\" to cause a non-malicious byzantine failure in replica group one and \"2\" to do the same for replica group two -- ");
    while (true) {
      String inp = keyb.nextLine();
      if (inp.trim().equals("1")) {
        port = 2222;
        break;
      } else if (inp.trim().equals("2")) {
        port = 3333;
        break;
      }
      System.out.println("Invalid choice! Enter a valid option:");
    }
    makeInvalidResultsOnReplica(port);
  }

  private static void makeInvalidResultsOnReplica(int port) {
    DatagramSocket aSocket = null;
    String reqOp = "REPLICA_BREAKER%";
    try {
      aSocket = new DatagramSocket();
      aSocket.setSoTimeout(5000);
      byte[] m = reqOp.getBytes();
      InetAddress aHost = InetAddress.getByName("127.0.0.1");
      DatagramPacket request = new DatagramPacket(m, reqOp.length(), aHost, port);
      aSocket.send(request);
      System.out.println("Successfully initiated replicaGroup on port " + port
          + " corruption sequence on all three region servers!");
    } catch (SocketTimeoutException e) {
      String timeOut = String.format("ERR: Request to server on port %d has timed out!", port);
      System.out.println(timeOut);
    } catch (SocketException e) {
      System.out.println("ERR: " + e.getMessage());
    } catch (IOException e) {
      System.out.println("ERR: " + e.getMessage());
    } finally {
      if (aSocket != null)
        aSocket.close();
    }
  }

  private static void runRegionUdpServer() {
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    executorService.execute((Runnable) () -> {
      String log =
          String.format("Starting UDP Server for breaker to receive confirmation on port %d...",
              UDP_BREAKER_PORT);
      System.out.println(log);
      listenForServerRequests();
    });

  }

  private static void listenForServerRequests() {
    // UDP server awaiting requests from other game servers
    DatagramSocket aSocket = null;
    try {
      aSocket = new DatagramSocket(UDP_BREAKER_PORT);
      byte[] buffer = new byte[65508];
      while (true) {
        DatagramPacket request = new DatagramPacket(buffer, buffer.length);
        aSocket.receive(request);
        String stringRequest = new String(request.getData(), 0, 7, StandardCharsets.UTF_8);
        // get confirmation of successful break
        if (stringRequest.equals("success")) {
          System.out.println("Exiting as failure on replica group has been initiated");
          System.exit(0);
        }
      }
    } catch (SocketException e) {
      System.out.println("Socket Exception: " + e.getMessage());
    } catch (IOException e) {
      System.out.println("IO Exception: " + e.getMessage());
    } finally {
      if (aSocket != null)
        aSocket.close();
    }
  }

}
