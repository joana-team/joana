package de.uni.trier.infsec.lib.network;

import de.uni.trier.infsec.environment.Environment;
import edu.kit.joana.ui.annotations.Sink;

/**
 * @author Enrico Scapin
 *
 */
public class Network {
	
	
	/**
	 * Server side: open a server socket able to accept connection 
	 * from a port.
	 * 
	 * @param port
	 * @throws NetworkError
	 */
	public static void openServerSocket(int port) throws NetworkError
	{
		// 1. transmit the unique identifier of this method to the environment
		Environment.untrustedOutput(0x2400);
		// 2. forward the input(s) of this method to the environment
		Environment.untrustedOutput(port);
		// 3. forward the output(s) of this environment to the system under verification
		if ( Environment.untrustedInput()==0 ) 
			throw new NetworkError();
	}
	
	/**
	 * Server side: wait for a new connection from a client
	 * 
	 * @param port
	 * @return socketID
	 * @throws NetworkError
	 */
	public static long acceptConnection(int port) throws NetworkError
	{
		// 1. transmit the unique identifier of this method to the environment
		Environment.untrustedOutput(0x2401);
		// 2. forward the input(s) of this method to the environment
		Environment.untrustedOutput(port);
		// 3. forward the output(s) of this environment to the system under verification
		if ( Environment.untrustedInput()==0 ) 
			throw new NetworkError();
		return Environment.untrustedInput();	
	}
	
	
	
	/**
	 * Server side: close the socket server
	 * @param port
	 * @throws NetworkError
	 */
	public static void closeServerSocket(int port) throws NetworkError
	{
		// 1. transmit the unique identifier of this method to the environment
		Environment.untrustedOutput(0x2402);
		// 2. forward the input(s) of this method to the environment
		Environment.untrustedOutput(port);
		// 3. forward the output(s) of this environment to the system under verification
		if ( Environment.untrustedInput()==0 ) 
			throw new NetworkError();
	}
	
	
	/**
	 * Client side: set a new connection to a server.
	 * 
	 * @param hostname server's hostname
	 * @param port
	 * @return socketID
	 * @throws NetworkError
	 */
	public static long openConnection(String hostname, int port) throws NetworkError
	{
		// 1. transmit the unique identifier of this method to the environment
		Environment.untrustedOutput(0x2403);
		// 2. forward the input(s) of this method to the environment
		Environment.untrustedOutputString(hostname);
		Environment.untrustedOutput(port);
		// 3. forward the output(s) of this environment to the system under verification
		if ( Environment.untrustedInput()==0 ) 
			throw new NetworkError();
		return Environment.untrustedInput();	
	}
	
	/**
	 * Server/Client side: close the socket
	 * 
	 * @param socketID
	 * @throws NetworkError
	 */
	public static void closeConnection(long socketID) throws NetworkError
	{
		// 1. transmit the unique identifier of this method to the environment
		Environment.untrustedOutput(0x2404);
		// 2. forward the input(s) of this method to the environment
		Environment.untrustedOutput(socketID);
		// 3. forward the output(s) of this environment to the system under verification
		if ( Environment.untrustedInput()==0 ) 
			throw new NetworkError();
	}
	
	
	/**
	 * Server/Client side: send a message through the socket
	 * @param socketID
	 * @param msg
	 * @throws NetworkError
	 */
	@Sink
	public static void sendMessage(long socketID, byte[] msg) throws NetworkError
	{
		// 1. transmit the unique identifier of this method to the environment
		Environment.untrustedOutput(0x2405);
		// 2. forward the input(s) of this method to the environment
		Environment.untrustedOutput(socketID);
		Environment.untrustedOutputMessage(msg);
		// 3. forward the output(s) of this environment to the system under verification
		if ( Environment.untrustedInput()==0 ) 
			throw new NetworkError();
	}
	
	/**
	 * Server/Client side: get a message from the socket
	 * 
	 * @param socketID
	 * @return
	 * @throws NetworkError
	 */
	public static byte[] getMessage(long socketID) throws NetworkError
	{
		// 1. transmit the unique identifier of this method to the environment
		Environment.untrustedOutput(0x2406);
		// 2. forward the input(s) of this method to the environment
		Environment.untrustedOutput(socketID);
		// 3. forward the output(s) of this environment to the system under verification
		if ( Environment.untrustedInput()==0 ) 
			throw new NetworkError();
		return Environment.untrustedInputMessage();
	}

}
