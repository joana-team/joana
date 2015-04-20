package rs3.cloud.functionalities.symenc;

import rs3.cloud.lib.crypto.CryptoLib;
import rs3.cloud.utils.MessageTools;

/**
 * Ideal functionality for private symmetric key encrytpion. 
 * 
 * This functionality is meant to be used, if a user wants to generate
 * a symmetric key to be used solely by her. The functionality provides
 * no means to share the key. The key is generated in the constructor
 * and never leaves the object.
 */
public class SymEnc {

	private byte[] key;
	private EncryptionLog log;
	
	public SymEnc() {
		key = CryptoLib.symkey_generateKey();
	}
	
	public byte[] encrypt(byte[] plaintext) {
		byte[] randomCipher = null;
		// keep asking the environment for the ciphertext, until a fresh one is given:
		while( randomCipher==null || log.containsCiphertext(randomCipher) ) {
			randomCipher = MessageTools.copyOf(CryptoLib.symkey_encrypt(MessageTools.copyOf(key), MessageTools.getZeroMessage(plaintext.length)));
		}
		log.add(MessageTools.copyOf(plaintext), randomCipher);
		return MessageTools.copyOf(randomCipher);		
	}
	
	public byte[] decrypt(byte[] ciphertext) { 
		if (!log.containsCiphertext(ciphertext)) {
			return MessageTools.copyOf( CryptoLib.symkey_decrypt(MessageTools.copyOf(key), MessageTools.copyOf(ciphertext)) );
		} else {
			return MessageTools.copyOf( log.lookup(ciphertext) );
		}			
	}
	
	/// IMPLEMENTATION ///
	
	private static class EncryptionLog {

		private static class MessagePairList {
			byte[] plaintext;
			byte[] ciphertext;
			MessagePairList next;
			public MessagePairList(byte[] plaintext, byte[] ciphertext, MessagePairList next) {
				this.ciphertext = ciphertext;
				this.plaintext = plaintext;
				this.next = next;
			}
		}

		private MessagePairList first = null;

		public void add(byte[] plaintext, byte[] ciphertext) {
			first = new MessagePairList(ciphertext, plaintext, first);
		}

		byte[] lookup(byte[] ciphertext) {
			for( MessagePairList node = first;  node != null;  node = node.next ) {
				if( MessageTools.equal(node.ciphertext, ciphertext) )
					return node.plaintext;
			}
			return null;
		}

		boolean containsCiphertext(byte[] ciphertext) {
			return lookup(ciphertext) != null;
		}
	}
}
