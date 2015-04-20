package rs3.clientserver.protocol;

import rs3.clientserver.pkenc.Decryptor;
import rs3.clientserver.pkenc.Encryptor;
import rs3.clientserver.protocol.Client;
import rs3.clientserver.protocol.Server;
import rs3.clientserver.network.Network;
import rs3.clientserver.network.NetworkError;

/**
 *  Setup for the simple protocol: it creates the server and then,
 *  depending on the input from the untrusted network, creates some
 *  (potentially unbounded) number of clients and makes them send
 *  their messages. 
 *
 *  In case of each client, two messages are determined by the
 *  environment; one of them is picked and sent by the client,
 *  depending on the value of a secret bit. The adversary is not
 *  supposed to learn the value of this bit.
 * 
 *  @author Andreas Koch (University of Trier)
 *  @author Tomasz Truderung (University of Trier)
 */
public class Setup {
	
	static private boolean secret = false; // SECRET -- an arbitrary value put here
		
	public static void main(String[] args) throws NetworkError {		
				
		// Public-key encryption functionality for Server 
		Decryptor serverDec = new Decryptor();
		Encryptor serverEnc = serverDec.getEncryptor();
		Network.networkOut(serverEnc.getPublicKey()); // the public key of Bob is published
		
		// Creating the server
		Server server = new Server(serverDec);
		
		// The adversary decides how many clients we create:
		while( Network.networkIn() != null )  { 
			// determine the value the client encrypts:
			// the adversary gives two values
			byte s1 = Network.networkIn()[0]; 
			byte s2 = Network.networkIn()[0];
			// and one of them is picked depending on the value of the secret bit
			byte s = secret ? s1 : s2; 
			Client client = new Client(serverEnc, s);
			
			// initialize the client protocol (Alice sends out an encrypted value s to the network)
			client.onInit();
			// read a message from the network...
			byte[] message = Network.networkIn();
			// ... and deliver it to the server (server will decrypt it)
			server.onReceive(message);			
		}
	}
}
