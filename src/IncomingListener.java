/**
 * A thread that listens for incoming peer connections and hands them over to
 * the client for handling
 * 
 * @author Philip Rego, Jess Calabretta, Jane Mehlig 
 * 
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class IncomingListener extends Thread {
	private final RUBTClient client;
	private ServerSocket sSocket;
	private boolean keepRunning = true;
	private int port;

	public void shutdown() {
		this.keepRunning = false;
		try {
			if (this.sSocket != null) {
				this.sSocket.close();
			}
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

	public IncomingListener(RUBTClient client) {
		this.client = client;
		this.sSocket = null;
	}

	public void run() {		
		if(FindOpenPort()==true){
			try {
				while (this.keepRunning) {
					Socket clientSocket = this.sSocket.accept();
					this.client.handleIncomingClient(clientSocket); //TODO Incoming connections  
				}
			} catch (Exception e) {
				System.err.println("Error on server socket!");
				e.printStackTrace();
			} finally {
				this.shutdown();
			}
		}
	}

	public boolean FindOpenPort(){	
		for(int port = 6880; port < 6890; port++)
		{
		   try
		   {
			   this.sSocket = new ServerSocket(port);
			   System.out.println(port + " Worked");
		       this.port=port;
			   return true;
		   }
		   catch(Exception e)
		   {
			   System.out.println(port + " didn't work");
		   }
		}
		return false;
	}
	
	public int getPort(){
		return port;
	}

}
