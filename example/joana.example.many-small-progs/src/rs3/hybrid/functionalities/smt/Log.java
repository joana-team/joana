package rs3.hybrid.functionalities.smt;


// A queue of messages along with the identifier of senders and receivers.
class Log 
{
	private static class Node {
		final LogEntry msg;
		final Log.Node next;

		Node(LogEntry msg, Log.Node next) {
			this.msg = msg;
			this.next = next;
		}
	}		
	private Log.Node first = null;

	void add(LogEntry msg) {
		first = new Node(msg, first);
	}

	LogEntry get(int index) {
		int i = 0;
		for( Log.Node node = first;  node != null;  node = node.next ) {
			if(i == index) return node.msg;
			++i;
		}
		return null;
	}
}