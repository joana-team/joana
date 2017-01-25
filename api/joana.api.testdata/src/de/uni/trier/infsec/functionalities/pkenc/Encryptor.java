package de.uni.trier.infsec.functionalities.pkenc;

import static de.uni.trier.infsec.utils.MessageTools.copyOf;
import static de.uni.trier.infsec.utils.MessageTools.getZeroMessage;

import de.uni.trier.infsec.lib.crypto.CryptoLib;

/**
 * Ideal functionality for public-key encryption: Encryptor
 */
public final class Encryptor
{

	private MessagePairList log;
	private byte[] publKey;
	
	Encryptor(MessagePairList log, byte[] publKey) 
	{ 
		this.log = log;		
		this.publKey = publKey;
	}
		
	public byte[] getPublicKey() 
	{
		return copyOf(publKey);
	}
	
	public byte[] encrypt(byte[] message) 
	{
		byte[] messageCopy = copyOf(message);
		byte[] randomCipher = null;
		// keep asking the environment for the ciphertext, until a fresh one is given:
		while( randomCipher==null || log.contains(randomCipher) ) {
			randomCipher = copyOf(CryptoLib.pke_encrypt(getZeroMessage(message.length), copyOf(publKey)));
		}
		log.add(messageCopy, randomCipher);
		return copyOf(randomCipher);
	}
}
