package rs3.clientserver.crypto;

import rs3.clientserver.environment.Environment;

/**
 *  @author Andreas Koch (University of Trier)
 *  @author Tomasz Truderung (University of Trier)
 */
public class CryptoLib {

	public static byte[] encrypt(byte[] in, byte[] publKey) {
		// input
		Environment.untrustedOutput(0x66); // Function code for encryption
		Environment.untrustedOutput(in.length);
		for (int i = 0; i < in.length; i++) {
			byte b = in[i];
			Environment.untrustedOutput(b);
		}
		Environment.untrustedOutput(publKey.length);
		for (int i = 0; i < publKey.length; i++) {
			byte b = publKey[i];
			Environment.untrustedOutput(b);
		}
		
		// output
		int len = Environment.untrustedInput();
		if (len<0) return null;
		byte[] returnval = new byte[len];
		for (int i = 0; i < len; i++) {
			returnval[i] = (byte) Environment.untrustedInput();
		}
		return returnval;
	}

	public static byte[] decrypt(byte[] message, byte[] privKey) {
		// input
		Environment.untrustedOutput(0x77); // Function code for decryption
		Environment.untrustedOutput(message.length);
		for (int i = 0; i < message.length; i++) {
			byte b = message[i];
			Environment.untrustedOutput(b);			
		}
		Environment.untrustedOutput(privKey.length);
		for (int i = 0; i < privKey.length; i++) {
			byte b = privKey[i];
			Environment.untrustedOutput(b);
		}
		
		// output
		int len = Environment.untrustedInput();
		if (len<0) return null;
		byte[] returnval = new byte[len];
		for (int i = 0; i < len; i++) {
			returnval[i] = (byte) Environment.untrustedInput();
		}
		return returnval;
	}

	public static KeyPair generateKeyPair() {
		// input
		Environment.untrustedOutput(0x88); // Function code for generateKeyPair
		
		// ouptut
		KeyPair returnval = new KeyPair();
		returnval.privateKey = null;
		int len = Environment.untrustedInput();
		if (len>=0) {
			returnval.privateKey = new byte[len];
			for (int i = 0; i < len; i++) {
				returnval.privateKey[i] = (byte) Environment.untrustedInput();
			}
		}
		returnval.publicKey = null;
		len = Environment.untrustedInput();
		if (len>=0) {
			returnval.publicKey= new byte[len];
			for (int i = 0; i < len; i++) {
				returnval.publicKey[i] = (byte) Environment.untrustedInput();
			}
		}
		return returnval;
	}	
}
