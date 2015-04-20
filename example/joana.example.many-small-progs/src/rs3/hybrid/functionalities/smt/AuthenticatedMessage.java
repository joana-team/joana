package rs3.hybrid.functionalities.smt;

/** 
 * Pair (message, sender_id).
 * 
 * Objects of this class are returned when a receiver gets a message.
 */
//@ spec_public
final public class AuthenticatedMessage {
	public final byte[] message;
	public final int sender_id;
	//@ public invariant message.length > 0;

	public AuthenticatedMessage(byte[] message, int sender) {
		this.sender_id = sender;  this.message = message;
	}
}