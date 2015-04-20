package rs3.hybrid.eVotingVerif.core;

import rs3.hybrid.functionalities.amt.AMT;
import rs3.hybrid.functionalities.amt.AMT.AMTError;
import rs3.hybrid.utils.MessageTools;

/*
 * Bulletin board on which the server can post messages (the result) and
 * everybody can retrieve the posted messages.
 */
public class BulletinBoard {

	public BulletinBoard() throws AMT.ConnectionError {
		content = new MessageList();
		AMT.listenOn(Params.LISTEN_PORT_BBOARD_AMT);
	}

	/*
	 * Reads a message, checks if it comes from the server, and, if this is the
	 * case, adds it to the maintained list of messages.
	 */
	public void onPost() throws AMTError {
		AMT.AuthenticatedMessage am = AMT.getMessage(Params.BULLETIN_BOARD_ID, Params.LISTEN_PORT_BBOARD_AMT);
		if (am == null) return;
		if (am.sender_id != Params.SERVER_ID) return;
		byte[] message = am.message;
		content.add(message);
	}

	/*
	 * Sends its content (that is the concatenation of all the message in the maintained
	 * list of messages) over the network.
	 */
	public byte[] onRequestContent() {
		byte[] contentMessage = {};
		for( MessageList.Node node = content.first;  node!=null;  node = node.next ) {
			contentMessage = MessageTools.concatenate(contentMessage, node.message);
		}
		return contentMessage;
	}

	/// implementation ///

	class MessageList {
		class Node {
			byte[] message;
			Node next;
			Node(byte[] message, Node next) { this.message = message; this.next = next; }
		}

		Node first = null;

		void add(byte[] message) {
			first = new Node(message, first);
		}
	}

	private MessageList content;
}
