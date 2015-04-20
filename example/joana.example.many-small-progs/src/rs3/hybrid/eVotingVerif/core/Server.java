package rs3.hybrid.eVotingVerif.core;

import rs3.hybrid.functionalities.smt.AuthenticatedMessage;
import rs3.hybrid.functionalities.smt.Receiver;
import rs3.hybrid.functionalities.smt.SMT;
import rs3.hybrid.functionalities.smt.SMT.SMTError;
import rs3.hybrid.functionalities.amt.AMT;
import rs3.hybrid.functionalities.amt.AMT.AMTError;

public final class Server {
	private final int numberOfVoters;
    private final int numberOfCandidates;
	private final boolean[] ballotCast;  // ballotCast[i]==true iff the i-th voter has already cast her ballot
	private final int[] votesForCandidates;
	private final Receiver receiver;
	private final AMT.Sender sender;

	//@ invariant \disjoint(SMT.rep, this.*);
	//@ invariant numberOfVoters == ballotCast.length;
	//@ invariant numberOfCandidates == votesForCandidates.length;
	//@ invariant 0 <= numberOfVoters;
	//@ invariant 0 <= numberOfCandidates;

	/*@ ensures numberOfVoters == this.numberOfVoters;
	  @ ensures numberOfCandidates == this.numberOfCandidates;
	  @ ensures receiver == this.receiver;
	  @ ensures sender == this.sender;
	  @ ensures (\forall int i; 0 <= i && i < numberOfVoters; !ballotCast[i]);
	  @ ensures (\forall int i; 0 <= i && i < numberOfCandidates; votesForCandidates[i]==0);
	  @ pure
	  @*/
	public Server(int numberOfVoters, int numberOfCandidates, 
			      Receiver receiver, AMT.Sender sender_to_BB) throws AMTError, SMT.ConnectionError {
		this.numberOfVoters = numberOfVoters;
		this.numberOfCandidates = numberOfCandidates;
		this.receiver = receiver;
		this.sender = sender_to_BB;
		votesForCandidates = new int[numberOfCandidates];		
		ballotCast = new boolean[numberOfVoters]; // initially no voter has cast her ballot
		receiver.listenOn(Params.LISTEN_PORT_SERVER_SMT);
	}

	/*
	 * Collect one ballot (read from a secure channel)
	 */
	public void onCollectBallot() throws SMTError {
		AuthenticatedMessage authMsg = receiver.getMessage(Params.LISTEN_PORT_SERVER_SMT);
		if (authMsg != null)
            onCollectBallot(authMsg);
	}
	
	/*@ 
	  @ ensures 0 <= authMsg.sender_id && authMsg.sender_id < numberOfVoters && !ballotCast[authMsg.sender_id]
	  @	&& authMsg.message != null && authMsg.message.length == 1
	  @	==> (votesForCandidates[authMsg.message[0]] == \old(votesForCandidates[authMsg.message[0]])+1
	  @         && (\forall int i; 0 <= i && i < numberOfCandidates; i != authMsg.message[0] ==>
	  @		votesForCandidates[i] == \old(votesForCandidates[i])));
	  @ assignable votesForCandidates[*];
          @*/
	private void onCollectBallot(AuthenticatedMessage authMsg) {
		if (authMsg == null) return;
		int voterID = authMsg.sender_id;
		byte[] ballot = authMsg.message;

		if( voterID<0 || voterID>=numberOfVoters ) return;  // invalid  voter ID
		if( ballotCast[voterID] ) return;  // the voter has already voted
		ballotCast[voterID] = true; 
		if( ballot==null || ballot.length!=1 ) return;  // malformed ballot
		int candidate = ballot[0];
		if (candidate >= 0 || candidate < numberOfCandidates ) {
			try {
			votesForCandidates[candidate]++;
			} catch (Exception e) {}
		}
	}
	
	/*
	 * Returns true if the result is ready, that is if all the eligible voters have already voted.
	 */
	/*@ normal_behavior
	  @ ensures \result ==
	  @	(\forall int i; 0 <= i && i < numberOfVoters; ballotCast[i]);
	  @ strictly_pure
	  @*/
	public boolean resultReady() {
		for( int i=0; i<numberOfVoters; ++i ) {
			if( !ballotCast[i] )
				return false;
		}
		return true;
	}
	
	/*
	 * Post the result (if ready) on the bulletin board.
	 */
	/*@ ensures true;
	  @ assignable SMT.rep, Environment.counter;
	  @*/
	public void onPostResult() throws AMTError, AMT.RegistrationError, AMT.ConnectionError {
		byte[] _result = getResult();
		if (_result != null)
			sender.sendTo(_result, Params.BULLETIN_BOARD_ID, 
					      Params.DEFAULT_HOST_BBOARD, Params.LISTEN_PORT_BBOARD_AMT);		
	}

	private byte[] getResult() {
		if (!resultReady()) return null; // the result is only returned when all the voters have voted
		
        int[] _result = new int[numberOfCandidates];
        for (int i=0; i<numberOfCandidates; ++i) {
            int x = votesForCandidates[i];
            // CONSERVATIVE EXTENSION:
            // PROVE THAT THE FOLLOWING ASSINGMENT IS REDUNDANT
            x = consExt(i);
            _result[i] = x;
        }
        return formatResult(_result);
	}

	/*@ ensures \result == votesForCandidates[i];
	  @ strictly_pure
	  @*/
    private int consExt(int i) {
        return Setup.correctResult[i];
    }

	/*
	 * Format the result of the election.
	 */
	/*@ ensures true;
	  @ pure
	  @*/
	private static byte[] formatResult(int[] _result) {
		String s = "Result of the election:\n";
		for( int i=0; i<_result.length; ++i ) {
			s += "  Number of votes for candidate " + i + ": " + _result[i] + "\n";
		}
		return s.getBytes();
	}
}
