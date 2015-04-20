package rs3.cloud.functionalities.pkisig;

import rs3.cloud.lib.crypto.CryptoLib;
import rs3.cloud.lib.crypto.KeyPair;
import rs3.cloud.utils.MessageTools;

/**
 * An object encapsulating a signing/verification key pair and allowing a user to
 * create a signature. In this implementation, when a message is signed, a real signature
 * is created (by an algorithm provided in lib.crypto) an the pair message/signature
 * is stores in the log.
 */
final public class Signer {
	private byte[] verifKey;
	private byte[] signKey;
	private Log log;

	public Signer() {
		KeyPair keypair = CryptoLib.generateSignatureKeyPair();
		this.signKey = MessageTools.copyOf(keypair.privateKey);
		this.verifKey = MessageTools.copyOf(keypair.publicKey);
		this.log = new Log();
	}

	public byte[] sign(byte[] message) {
		byte[] signature = CryptoLib.sign(MessageTools.copyOf(message), MessageTools.copyOf(signKey));
		// we make sure that the signing has not failed
		if (signature == null) return null;
		// and that the signature is correct
		if( !CryptoLib.verify(MessageTools.copyOf(message), MessageTools.copyOf(signature), MessageTools.copyOf(verifKey)) )
			return null;
		// now we log the message (only!) as signed and return the signature
		log.add(MessageTools.copyOf(message));
		return MessageTools.copyOf(signature);
	}

	public Verifier getVerifier() {
		return new UncorruptedVerifier(verifKey, log);
	}
	
	///// IMPLEMENTATION /////
	
	static class Log {

		private static class MessageList {
			byte[] message;
			MessageList next;
			public MessageList(byte[] message, MessageList next) {
				this.message = message;
				this.next = next;
			}
		}

		private MessageList first = null;

		public void add(byte[] message) {
			first = new MessageList(message, first);
		}

		boolean contains(byte[] message) {
			for( MessageList node = first;  node != null;  node = node.next ) {
				if( MessageTools.equal(node.message, message) )
					return true;
			}
			return false;
		}
	}
	
}
