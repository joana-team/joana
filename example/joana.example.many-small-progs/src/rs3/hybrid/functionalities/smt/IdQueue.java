package rs3.hybrid.functionalities.smt;


// Collection of (registered) identifiers.
class IdQueue
{	
	private static class Node {
		final int id;
		final IdQueue.Node next;

		Node(int id, IdQueue.Node next) {
			this.id = id;
			this.next = next;
		}
	}			
	private IdQueue.Node first = null;

	public void add(int id) {
		first = new Node(id, first);
	}

	boolean exists(int id) {
		for( IdQueue.Node node = first;  node != null;  node = node.next )
			if( id == node.id )
				return true;
		return false;
	}
}