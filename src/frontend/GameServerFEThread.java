package frontend;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class GameServerFEThread extends Thread {
	private DatagramSocket frontEndSocket;
	private DatagramPacket request;
	private byte [] buffer;
	private String [] messageArray;
	private int port;
	private boolean threadDown;
	
	protected GameServerFEThread(int port) 
	{
		this.port = port;
		threadDown = false;
		buffer = new byte [1200];
		try {
			frontEndSocket = new DatagramSocket(port);
		} catch (SocketException e) {
			threadDown = true;
		}
	}
	
	protected boolean hasCrashed()
	{
		return threadDown;
	}
	
	@Override
	public void run ()
	{
		while(true) {
			try {
				request = new DatagramPacket(buffer, buffer.length);
				frontEndSocket.receive(request);
				messageArray = (new String(request.getData())).split("/");
				if(messageArray[0].equals("LR"))
				{
					GameServerORBThread.setConfimation(messageArray[1]);
					GameServerORBThread.setLeaderResponded(true);
				}
			} catch (IOException e) {
				frontEndSocket.close();
				threadDown = true;
			}
		}
	}
}
