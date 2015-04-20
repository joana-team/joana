package rs3.hybrid.functionalities.smt;

import rs3.hybrid.environment.SMTEnv;

/**
 * Ideal functionality for SMT (Secure Authenticated Message Transmission).
 */
public final class SMT {

	//// The public interface ////

	static public class SMTError extends Exception {}

	static public class ConnectionError extends Exception {}

	static public class RegistrationError extends Exception {}

	// what locations belong to the network/SMT, what may be changed upon sending
	//@ public static ghost \locset rep;

	// the abstract state (message queue)
	//@ public static ghost \seq receiver_ids;
	//@ public static ghost \seq messages;
	//@ public static ghost \seq sender_ids;

	//@ public static invariant receiver_ids.length == sender_ids.length;
	//@ public static invariant receiver_ids.length == messages.length;

	//@ public static ghost \seq registered_sender_ids;
	//@ public static ghost \seq registered_receiver_ids;

	/*@ ensures \invariant_for(\result) && \fresh(\result);
	  @ ensures \new_elems_fresh(SMT.rep);
	  @ ensures SMT.registered_sender_ids == \seq_concat(\old(SMT.registered_sender_ids),\seq_singleton(id));
	  @ assignable SMT.rep, SMT.registered_sender_ids;
	  @*/
	public static Sender registerSender(int id) throws SMTError, RegistrationError, ConnectionError {
		if (registrationInProgress) throw new SMTError();
		registrationInProgress = true;
		// call the simulator, throw a network error if the simulator says so
		boolean network_ok = SMTEnv.registerSender(id);
		if (!network_ok) throw new ConnectionError();
		// check whether the id has not been claimed
		if( registeredSenders.exists(id) ) {
			registrationInProgress = false;
			throw new RegistrationError();
		}
		// create a new agent, add it to the list of registered agents, and return it
		registeredSenders.add(id);
		Sender sender = new Sender(id);
		registrationInProgress = false;
		return sender;
	}

	/*@ ensures \invariant_for(\result) && \fresh(\result);
	  @ ensures \new_elems_fresh(SMT.rep);
	  @ ensures SMT.registered_receiver_ids == \seq_concat(\old(SMT.registered_receiver_ids),\seq_singleton(id));
	  @ assignable SMT.rep;
	  @*/
	public static Receiver registerReceiver(int id) throws SMTError, RegistrationError, ConnectionError {
		if (registrationInProgress) throw new SMTError();
		registrationInProgress = true;
		// call the simulator, throw a network error if the simulator says so
		boolean network_ok =  SMTEnv.registerReceiver(id);
		if (!network_ok) throw new ConnectionError();
		// check whether the id has not been claimed
		if( registeredReceivers.exists(id) ) {
			registrationInProgress = false;
			throw new RegistrationError();
		}
		// create a new agent, add it to the list of registered agents, and return it
		registeredReceivers.add(id);
		Receiver receiver = new Receiver(id);
		registrationInProgress = false;
		return receiver;
	}


	//// Implementation ////

	static boolean registrationInProgress = false;

	static Log log = new Log();

	// static lists of registered agents:
	private static IdQueue registeredSenders = new IdQueue();
	static IdQueue registeredReceivers = new IdQueue();
}
