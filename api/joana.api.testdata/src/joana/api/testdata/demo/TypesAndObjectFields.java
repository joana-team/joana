package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class TypesAndObjectFields {

	static class NodeA {
		public NodeB next;
		public int val;

		public NodeA(int val) {
			this.val = val;
		}
	}
	static class NodeB {
		public NodeA next;
		public int val;

		public NodeB(int val) {
			this.val = val;
		}
	}
	
	public static void main(String[] argv) {
		NodeA a = new NodeA(secret());
		NodeB b = new NodeB(input());
		b.next = a;
		a.next = b;

		// ok
		print(b.val);
		// illegal
		print(a.val);
		print(b.next.val);
		// ok
		print(a.next.val);
	}

	@Sink
	public static void print(int s) {}
	@Source
	public static int secret() { return 42; };
	public static int input() { return 23; };
	
}