package rs3.hybrid.lib.network;

import rs3.hybrid.environment.Environment;

public class NetworkServer {

	public static void listenForRequests(int port) throws NetworkError {
		// input
		Environment.untrustedOutput(0x2400);
		Environment.untrustedOutput(port);
		// output
		if ( Environment.untrustedInput()==0 ) throw new NetworkError();
	}

	public static byte[] nextRequest(int port) throws NetworkError {
		// input
		Environment.untrustedOutput(0x2401);
		Environment.untrustedOutput(port);
		// output
		if ( Environment.untrustedInput()==0 ) throw new NetworkError();
		return Environment.untrustedInputMessage();
	}

	public static void response(byte[] message) throws NetworkError {
		// input
		Environment.untrustedOutput(0x2402);
		Environment.untrustedOutputMessage(message);
		// output
		if ( Environment.untrustedInput()==0 ) throw new NetworkError();		
	}

	public static byte[] read(int port) throws NetworkError {
		// input
		Environment.untrustedOutput(0x2403);
		Environment.untrustedOutput(port);
		// output
		if ( Environment.untrustedInput()==0 ) throw new NetworkError();
		return Environment.untrustedInputMessage();		
	}
}
