package de.uni.trier.infsec.functionalities.pkenc;

import de.uni.trier.infsec.utils.MessageTools;

public class MessagePairList 
{
	
	static class MessagePair 
	{
		byte[] plaintext;
		byte[] ciphertext;
		MessagePair next;
		public MessagePair(byte[] plaintext, byte[] ciphertext, MessagePair next) {
			this.plaintext = plaintext;
			this.ciphertext = ciphertext;
			this.next = next;
		}
	}
	
	
	private MessagePair first = null;
	
	public synchronized void add(byte[] plaintext, byte[] ciphertext)
	{
		first = new MessagePair(plaintext, ciphertext, first);
	}

    public synchronized byte[] lookup(byte[] ciphertext) 
    {
        MessagePair tmp = first;
        while(tmp!=null){
            if(MessageTools.equal(tmp.ciphertext, ciphertext))
                return tmp.plaintext;
            tmp = tmp.next;
        }
        return null;
    }
    
    public synchronized boolean contains(byte[] ciphertext) 
    {
        MessagePair tmp = first;
        while(tmp!=null){
            if(MessageTools.equal(tmp.ciphertext, ciphertext))
                return true;
            tmp = tmp.next;
        }
        return false;
    }
    
}
