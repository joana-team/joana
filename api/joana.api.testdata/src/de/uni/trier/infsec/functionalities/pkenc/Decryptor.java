package de.uni.trier.infsec.functionalities.pkenc;

import de.uni.trier.infsec.lib.crypto.CryptoLib;
import de.uni.trier.infsec.lib.crypto.KeyPair;
import static de.uni.trier.infsec.utils.MessageTools.copyOf;

/**
 * Ideal functionality for public-key encryption: Decryptor
 */
public final class Decryptor 
{
	
	private byte[] privKey; 
	private byte[] publKey;
	private MessagePairList log = new MessagePairList();

	public Decryptor() 
	{
		KeyPair keypair = CryptoLib.pke_generateKeyPair();
		publKey = copyOf(keypair.publicKey);  
		privKey = copyOf(keypair.privateKey); 
	}

    public Encryptor getEncryptor() 
    {
        return new Encryptor(log,publKey);
    }

	public byte[] decrypt(byte[] message) 
	{
		byte[] messageCopy = copyOf(message); 
		if (!log.contains(messageCopy)) {
			return copyOf( CryptoLib.pke_decrypt(copyOf(privKey), messageCopy) );
		} else {
			return copyOf( log.lookup(messageCopy) );
		}
	}
}
