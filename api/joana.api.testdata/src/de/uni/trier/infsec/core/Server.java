package de.uni.trier.infsec.core;

import de.uni.trier.infsec.functionalities.pkenc.Decryptor;
import de.uni.trier.infsec.lib.network.Network;
import de.uni.trier.infsec.lib.network.NetworkError;

public class Server implements Runnable
{
	private int port;
	private Decryptor decr;
	boolean listening;
	
	public Server(Decryptor decr, int port){
		this.decr=decr;
		this.port=port;
		listening=true;
	}
	
	public void run() 
	{
		try {
			Network.openServerSocket(port);
		} catch (NetworkError e) {
			e.printStackTrace();
			return;
		}

		while(listening){
			long socketID=0;
			try {
				// blocking call: the thread is suspended until a new connection is received
				socketID = Network.acceptConnection(port);
			} catch (NetworkError e) {
				e.printStackTrace();
				return;
			}
			// call the thread which handles the request
			RequestHandler handler = new RequestHandler(socketID, decr); 
			new Thread(handler).start();
		}
		try {
			Network.closeServerSocket(port);
		} catch (NetworkError e) {
			e.printStackTrace();
			return;
		}
	}
	
	public static class RequestHandler implements Runnable
	{
		private long socketID;
		private Decryptor decr;

		public RequestHandler(long requestID, Decryptor decr){
			this.socketID=requestID;
			this.decr=decr;
		}
		
		@Override
		public void run()
		{
			byte[] message = null;
			try {
				message = Network.getMessage(socketID);
				byte[] decrypted=decr.decrypt(message);
				Network.closeConnection(socketID);
			} catch (NetworkError e) {
				e.printStackTrace();
				return;
			}				
		}
	}

}