package rs3.hybrid.eVotingVerif.core;


public class Params {
	/*
	 * Agent identifiers.
	 */
	public static final int SERVER_ID = -1;
	public static final int BULLETIN_BOARD_ID = -2;
	// eligible voters get the identifiers in the range 0..Server.NumberOfVoters
	
	
	public static final int LISTEN_PORT_SERVER_SMT = 4089;		// Listen port for Voter requests
	public static final int LISTEN_PORT_BBOARD_AMT = 4090;		// Listen port for Server requests
	public static final int LISTEN_PORT_BBOARD_REQUEST = 4092;	// Listen port for result requests

	public static final String DEFAULT_HOST_SERVER = "localhost";
	public static final String DEFAULT_HOST_BBOARD = "localhost";
	
	// public static final int DEFAULT_LISTEN_PORT_SERVER_AMT = 4088;	// Listen port for Voter requests
	// public static final int DEFAULT_LISTEN_PORT_BBOARD_SMT = 4091;	// Listen port for Server requests
		
}
