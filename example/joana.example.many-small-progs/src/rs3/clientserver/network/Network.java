package rs3.clientserver.network;

import rs3.clientserver.environment.Environment;


/**
 *  @author Andreas Koch (University of Trier)
 *  @author Tomasz Truderung (University of Trier)
 */
public class Network {

	public static void networkOut(byte[] outEnc) throws NetworkError {
		// input
		Environment.untrustedOutput(0x55);
		Environment.untrustedOutput(outEnc.length);		
		for (int i = 0; i < outEnc.length; i++) {
			Environment.untrustedOutput(outEnc[i]);			
		}
		// output
		if (Environment.untrustedInput()==0) throw new NetworkError();
	}

	public static byte[] networkIn() throws NetworkError {
		// input
		Environment.untrustedOutput(0x56);
		
		// output
		if (Environment.untrustedInput()==0) throw new NetworkError();
		int len = Environment.untrustedInput();
		if (len<0) return null;
		byte[] val = new byte[len];
		for (int i = 0; i < len; i++) {
			val[i] = (byte) Environment.untrustedInput();
		}
		return val;
	}
}
