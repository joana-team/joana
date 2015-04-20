package rs3.cloud.lib.crypto;

import rs3.cloud.environment.Environment;

public class CryptoLib {

	public static byte[] nextNonce() {
		// input
		Environment.untrustedOutput(0x100);
		// output
		return Environment.untrustedInputMessage();
	}

	public static byte[] symkey_generateKey() {
		// input
		Environment.untrustedOutput(0x91);
		// output
		return Environment.untrustedInputMessage();
	}

	public static byte[] symkey_encrypt(byte[] key, byte[] msg) {
		// input
		Environment.untrustedOutput(0x92);
		Environment.untrustedOutputMessage(msg);
		Environment.untrustedOutputMessage(key);
		// output
		return Environment.untrustedInputMessage();
	}

	public static byte[] symkey_decrypt(byte[] key, byte[] msg) {
		// input
		Environment.untrustedOutput(0x93);
		Environment.untrustedOutputMessage(msg);
		Environment.untrustedOutputMessage(key);
		// output
		return Environment.untrustedInputMessage();
	}


	public static byte[] pke_encrypt(byte[] message, byte[] publKey) {
		// input
		Environment.untrustedOutput(0x66); // Function code for pke_encrypt
		Environment.untrustedOutputMessage(message);
		Environment.untrustedOutputMessage(publKey);
		// output
		return Environment.untrustedInputMessage();
	}

	public static byte[] pke_decrypt(byte[] message, byte[] privKey) {
		// input
		Environment.untrustedOutput(0x77); // Function code for pke_decrypt
		Environment.untrustedOutputMessage(message);
		Environment.untrustedOutputMessage(privKey);		
		// output
		return Environment.untrustedInputMessage();
	}
	
	public static KeyPair pke_generateKeyPair() {
		// input
		Environment.untrustedOutput(0x88); // Function code for pke_generateKeyPair
		
		// output
		KeyPair resval = null;
		if( Environment.untrustedInput()==0 ) {
			resval = new KeyPair();
			resval.privateKey = Environment.untrustedInputMessage();
			resval.publicKey = Environment.untrustedInputMessage();
		}
		return resval;
	}

	public static boolean verify(byte[] message, byte[] signature, byte[] pubKey) {
		// input
		Environment.untrustedOutput(0x22); // Function code for digital signature verification ds_verify
		Environment.untrustedOutputMessage(message);
		Environment.untrustedOutputMessage(signature);
		Environment.untrustedOutputMessage(pubKey);		
		// output
		return Environment.untrustedInput() != 0;
	}

	public static byte[] sign(byte[] message, byte[] signingKey) {
		// input
		Environment.untrustedOutput(0x66); // Function code for pke_encrypt
		Environment.untrustedOutputMessage(message);
		Environment.untrustedOutputMessage(signingKey);
		// output
		return Environment.untrustedInputMessage();
	}

	public static byte[] virifySignature(byte[] signature, byte[] message, byte[] verificationKey) {
		// input
		Environment.untrustedOutput(0x66); // Function code for pke_encrypt
		Environment.untrustedOutputMessage(message);
		Environment.untrustedOutputMessage(signature);
		Environment.untrustedOutputMessage(verificationKey);
		// output
		return Environment.untrustedInputMessage();
	}

	public static KeyPair generateSignatureKeyPair() {
		// input
		Environment.untrustedOutput(0x88); // Function code for generateSignatureKeyPair

		// ouptut
		KeyPair resval = null;
		if( Environment.untrustedInput()==0 ) {
			resval = new KeyPair();
			resval.privateKey = Environment.untrustedInputMessage();
			resval.publicKey = Environment.untrustedInputMessage();
		}
		return resval;
	}

}
