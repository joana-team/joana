package rs3.clientserver.pkenc;

import rs3.clientserver.crypto.CryptoLib;

/**
 * Ideal functionality for public-key encryption: Encryptor
 *
 *  @author Andreas Koch (University of Trier)
 *  @author Tomasz Truderung (University of Trier)
 */
public final class Encryptor {

	private MessagePairList log;
	private byte[] publKey;
	
	Encryptor(MessagePairList mpl, byte[] publicKey) { 
		log = mpl;		
		publKey = publicKey;
	}
		
	public byte[] getPublicKey() {
		return MessageTools.copyOf(publKey);
	}
	
	public byte[] encrypt(byte[] message) {
		byte[] messageCopy = MessageTools.copyOf(message);
		byte[] randomCipher = MessageTools.copyOf(CryptoLib.encrypt(MessageTools.getZeroMessage(1), 
																	MessageTools.copyOf(publKey))); // Note the fixed size (1) of a message
		if( randomCipher == null ) return null;
		log.add(messageCopy, randomCipher);
		return MessageTools.copyOf(randomCipher);
	}
	
}
