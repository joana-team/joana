package de.uni.trier.infsec.utils;


public class MessageTools {
	
	public static byte[] copyOf(byte[] message) {
		if (message==null) return null;
		byte[] copy = new byte[message.length];
		for (int i = 0; i < message.length; i++) {
			try {
				copy[i] = message[i];
			} catch (Exception e) {
				// cannot happen
			}
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
	
	/**
	 * Concatenates messages in a way that makes it possible to unambiguously
	 * split the message into the original messages (it adds length of the
	 * first message at the beginning of the returned message).
	 */
	public static byte[] concatenate(byte[] m1, byte[] m2) {
		// Concatenated Message --> byte[0-3] = Integer, Length of Message 1
		byte[] out = new byte[m1.length + m2.length + 4];

		// 4 bytes for length
		byte[] len = intToByteArray(m1.length);

		// copy all bytes to output array
		int j = 0;
		for( int i=0; i<len.length; ++i ) out[j++] = len[i];
		for( int i=0; i<m1.length;  ++i ) out[j++] = m1[i];
		for( int i=0; i<m2.length;  ++i ) out[j++] = m2[i];

		return out;
	}

	/**
	 * Simply concatenates the messages (without adding any information for
	 * de-concatenation).
	 */
	public static byte[] raw_concatenate(byte[] a, byte[] b) {
		byte[] result = new byte[a.length + b.length];
		int j = 0;
		for( int i=0; i<a.length; ++i ) result[j++] = a[i];
		for( int i=0; i<b.length; ++i ) result[j++] = b[i];
		return result;
	}

	/**
	 * Projection of the message to its two parts (part 1 = position 0, part 2 = position 1) Structure of the expected data: 1 Byte Identifier [0x01], 4 Byte
	 * length of m1, m1, m2
	 */
	private static byte[] project(byte[] message, int position) {
		try {
			int len = byteArrayToInt(message);
			if (len > (message.length - 4)) return new byte[]{}; // Something is wrong with the message!
			if (position == 0) {
				byte[] m1 = new byte[len];
				for (int i = 0; i < len; i ++) m1[i] = message[i + 4];
				return m1;
			} else if (position == 1) {
				byte[] m2 = new byte[message.length - len - 4];
				for (int i = 0; i < message.length - len - 4; i ++) m2[i] = message[i + 4 + len];
				return m2;
			} else return new byte[]{};
		} catch (Exception e) {
			return new byte[]{};
		}
	}

	public static byte[] first(byte[] in) {
		return project(in, 0);
	}

	public static byte[] second(byte[] in) {
		return project(in, 1);
	}

	public static final int byteArrayToInt(byte [] b) {
        return (b[0] << 24)
                + ((b[1] & 0xFF) << 16)
                + ((b[2] & 0xFF) << 8)
                + (b[3] & 0xFF);
	}
	

	public static final byte[] intToByteArray(int value) {
	        return new byte[] {
	                (byte)(value >>> 24),
	                (byte)(value >>> 16),
	                (byte)(value >>> 8),
	                (byte)value};
	}
}
