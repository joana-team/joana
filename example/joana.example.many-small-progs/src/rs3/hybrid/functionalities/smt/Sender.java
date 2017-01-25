package rs3.hybrid.functionalities.smt;

import rs3.hybrid.environment.SMTEnv;
import rs3.hybrid.functionalities.smt.SMT.ConnectionError;
import rs3.hybrid.functionalities.smt.SMT.RegistrationError;
import rs3.hybrid.functionalities.smt.SMT.SMTError;
import rs3.hybrid.lib.network.NetworkClient;
import rs3.hybrid.lib.network.NetworkError;
import rs3.hybrid.utils.MessageTools;

final public class Sender 
{
	/*@ invariant 
	  @ (\exists int i; 0 <= i && i < SMT.registered_sender_ids.length; (int)SMT.registered_sender_ids[i]==id);
	  @ invariant 
	  @ (\forall Sender s; s.id == id; s == this);
	  @ invariant \disjoint(SMT.rep, \singleton(this.id));
	  @*/
	public final int id;

	/*@ behavior // the following must be true if no exception is thrown
	  @ requires message.length > 0;
	  @ ensures SMT.messages == \seq_concat(\old(SMT.messages),\seq_singleton(message[0]));
	  @ ensures SMT.receiver_ids == \seq_concat(\old(SMT.receiver_ids),\seq_singleton(receiver_id));
	  @ ensures SMT.sender_ids == \seq_concat(\old(SMT.sender_ids),\seq_singleton(id));
	  @ ensures (\exists int i; 0 <= i && i < SMT.registered_receiver_ids.length; SMT.registered_receiver_ids[i]==receiver_id);
  	  @ ensures \new_elems_fresh(SMT.rep);
	  @ assignable SMT.rep, SMT.messages, SMT.receiver_ids, SMT.sender_ids, Environment.counter; // what can be changed
	  @*/
	public void sendTo(/*@nullable@*/ byte[] message, int receiver_id, /*@ nullable @*/ String server, int port) throws SMTError, RegistrationError, ConnectionError {
		if (SMT.registrationInProgress) throw new SMTError();

		// get from the simulator a message to be later sent out
		byte[] output_message = SMTEnv.sendTo(message.length, id, receiver_id, server, port);
		if (output_message == null) throw new ConnectionError();
		// get the answer from PKI
		if (!SMT.registeredReceivers.exists(receiver_id))
			throw new RegistrationError();
		// log the sent message along with the sender and receiver identifiers			
		SMT.log.add(new LogEntry(MessageTools.copyOf(message), id, receiver_id));
	  	//@ set SMT.messages = \seq_concat(SMT.messages,\seq_singleton(message[0]));
		//@ set SMT.receiver_ids = \seq_concat(SMT.receiver_ids,\seq_singleton(receiver_id));
		//@ set SMT.sender_ids = \seq_concat(SMT.sender_ids,\seq_singleton(id));

		// sent out the message from the simulator
		try {
			NetworkClient.send(output_message, server, port);
		}
		catch( NetworkError e ) {
			throw new ConnectionError();
		}
	}

	Sender(int id) {
		this.id = id;
	}
}