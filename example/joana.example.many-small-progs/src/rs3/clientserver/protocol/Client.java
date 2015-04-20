package rs3.clientserver.protocol;

import rs3.clientserver.pkenc.Encryptor;
import rs3.clientserver.network.Network;
import rs3.clientserver.network.NetworkError;
// import de.uni.trier.infsec.pkenc.PKEnc;

/**
 *  Client of a simple protocol: it encrypts a given message and sends
 *  it over the network.
 * 
 *
 *  @author Andreas Koch (University of Trier)
 *  @author Tomasz Truderung (University of Trier)
 */
final public class Client {
	private Encryptor BobPKE;
	private byte[] message;

	public Client(Encryptor BobPKE, byte message) {
		this.BobPKE = BobPKE;
		this.message = new byte[] {message}; 
	}

	public void onInit() throws NetworkError {
		byte[] encMessage = BobPKE.encrypt(message);
		Network.networkOut(encMessage);
	}
}
