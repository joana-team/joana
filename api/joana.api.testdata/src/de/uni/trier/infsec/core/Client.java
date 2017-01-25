package de.uni.trier.infsec.core;

import de.uni.trier.infsec.functionalities.pkenc.Encryptor;
import de.uni.trier.infsec.lib.network.Network;
import de.uni.trier.infsec.lib.network.NetworkError;

public class Client implements Runnable {
	
	private Encryptor enc;
	private byte[] msg;
	
	private String hostname;
	private int port;
	
	public Client(Encryptor enc, byte[] msg, String host, int port) {
		this.enc = enc;
		this.msg=msg;
		
		this.hostname=host;
		this.port=port;
	}
	
	@Override
	public void run()
	{
		byte[] mgs_enc = enc.encrypt(msg);
		
		long socketID=0;
		try {
			socketID = Network.openConnection(hostname, port);
			Network.sendMessage(socketID, mgs_enc);
			Network.closeConnection(socketID);
		} catch (NetworkError e) {
			e.printStackTrace();
			return;
		}
	}
}
