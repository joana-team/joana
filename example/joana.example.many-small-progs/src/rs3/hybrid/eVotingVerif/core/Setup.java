package rs3.hybrid.eVotingVerif.core;

import rs3.hybrid.environment.Environment;
import rs3.hybrid.functionalities.amt.AMT;
import rs3.hybrid.functionalities.smt.Receiver;
import rs3.hybrid.functionalities.smt.SMT;
import rs3.hybrid.functionalities.smt.Sender;

public final class Setup 
{
	private final Voter[] voters;
	private final Server server;
	private final BulletinBoard BB;

	// one secret bit
	private static boolean secret;

	// the correct result
	static int[] correctResult; // CONSERVATIVE EXTENSION

	private Setup(int numberOfCandidates, int numberOfVoters) throws Throwable {
		// let the environment determine two vectors of choices
		byte[] choices0 = new byte[numberOfVoters];
		byte[] choices1 = new byte[numberOfVoters];
		for (int i=0; i<numberOfVoters; ++i) {
			choices0[i] = (byte)(Environment.untrustedInput() % numberOfCandidates);
			choices1[i] = (byte)(Environment.untrustedInput() % numberOfCandidates);
		}

		// check that those vectors give the same result
		int[] r0 = computeResult(choices0, numberOfCandidates);
		int[] r1 = computeResult(choices1, numberOfCandidates);
		if (!equalResult(r0,r1))
			throw new Throwable();	// abort if the vectors do not yield the same result

		// store correct result
		correctResult = r1; // CONSERVATIVE EXTENSION

		// create voters, using the choices from the vectors
		// according to the secret bit        
		voters = new Voter[numberOfVoters];
		for( int i=0; i<numberOfVoters; ++i ) {
			Sender sender = SMT.registerSender(i); // sender with identifier i
			byte choice0 = choices0[i];
			byte choice1 = choices1[i];
			byte choice = secret ? choice0 : choice1;
			voters[i] = new Voter(choice, sender);
		}

		// create the server
		Receiver serverReceiver = SMT.registerReceiver(Params.SERVER_ID);
		AMT.Sender serverSender = AMT.registerSender(Params.SERVER_ID);
		server = new Server(numberOfVoters, numberOfCandidates, serverReceiver, serverSender);

		// create the bulletin board
		BB = new BulletinBoard();
	}

	private static int[] computeResult (byte[] choices, int numberOfCandidates) {
		int[] res = new int[numberOfCandidates];
		for (int i=0; i<choices.length; i++) 
			++res[choices[i]];
		return res;
	}

	private static boolean equalResult(int[] r1, int[] r2) {
		for (int j= 0; j<r1.length; j++)
			if (r1[j]!=r2[j]) return false;
		return true;
	}


	public static void main (String[] a) throws Throwable {
        int numberOfCandidates = Environment.untrustedInput();
        int numberOfVoters = Environment.untrustedInput();
        if (numberOfVoters<=0 || numberOfCandidates<=0)
			throw new Throwable();	// abort 
		Setup s = new Setup(numberOfCandidates, numberOfVoters);
        int N = Environment.untrustedInput(); // the environment decides how long the system runs
        s.votingPhase(N);
        s.afterVotingPhase();
	}

	public void votingPhase(int N) throws Throwable {
        int voter = 0;
        for(int i=0; i<N; ++i ) {
			int decision = Environment.untrustedInput();
            if (decision >= 0) { // a voter (determined by the adversary) votes
				final Voter v = voters[voter++]; // better: v = voters[decision]
				v.onSendBallot();
            }
            else { // the server reads a message from a secure channel
				server.onCollectBallot();
            }
        }
        server.onPostResult();
    }

    public void afterVotingPhase() throws Throwable {
        while( Environment.untrustedInput() != 0 ) {
			int decision = Environment.untrustedInput();
            if (decision >= 0){ // the bulletin board reads a message:
				BB.onPost();
            }
			else {
            	byte[] content = BB.onRequestContent();
            	Environment.untrustedOutputMessage(content);
            }
        }
    }

}
