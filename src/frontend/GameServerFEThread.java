package frontend;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class GameServerFEThread extends Thread {
	private DatagramSocket dgSocket;
	private DatagramPacket request;
	private byte [] buffer;
	private String [] messageArray;
	private int port;
	private boolean threadDown;
	
	protected GameServerFEThread(int port) 
	{
		this.port = port;
		threadDown = false;
		buffer = new byte [65535];
		try {
			dgSocket = new DatagramSocket(port);
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
		while(true)
			handleCommunication();
	}

	/*  Handles the messages received from the replica leader */
	private void handleCommunication() 
	{
		try {
			request = new DatagramPacket(buffer, buffer.length);
			dgSocket.receive(request);
			
			messageArray = (new String(request.getData())).split("/");
			
			if(messageArray[0].equals("LR"))
			{
				if(messageArray.length > 3) // get Player Status
				{
					//messageArray[1] is confirmation
					GameServerORBThread.setResponse(messageArray[2] + " Online " + messageArray[3] + " Offline " + messageArray[4] + "\n" +
							messageArray[5] + " Online " + messageArray[6] + " Offline " + messageArray[7] + "\n" +
							messageArray[8] + " Online " + messageArray[9] + " Offline " + messageArray[10]);
				}
				else
				{
					switch(Integer.parseInt(messageArray[1].substring(0, 1)))
					{
						case 0 : GameServerORBThread.setConfimation(false); break;
						case 1 : GameServerORBThread.setConfimation(true); break;
					}
				}
				GameServerORBThread.setLeaderResponded(true);
			}
		} catch (IOException e) {
			dgSocket.close();
			threadDown = true;
		}
	}	
}
