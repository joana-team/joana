package rs3.clientserver.pkenc;

/**
 *  @author Andreas Koch (University of Trier)
 *  @author Tomasz Truderung (University of Trier)
 */
public class MessageTools {
	
	public static byte[] copyOf(byte[] message) {
		if (message==null) return null;
		byte[] copy = new byte[message.length];
		for (int i = 0; i < message.length; i++) {
			copy[i] = message[i];
		}
		return copy;
	}

    public static boolean equal(byte[] a, byte[] b) {
        if( a.length != b.length ) return false;
        for( int i=0; i<a.length; ++i)
            if( a[i] != b[i] ) return false;
        return true;
    }			

	public static byte[] getZeroMessage(int messageSize) {
		byte[] zeroVector = new byte[messageSize];
		for (int i = 0; i < zeroVector.length; i++) {
			zeroVector[i] = 0x00;
		}
		return zeroVector;
	}	
}
