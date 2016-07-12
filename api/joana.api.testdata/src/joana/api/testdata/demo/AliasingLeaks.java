package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class AliasingLeaks {

	static class A {
		public B b;

		public A(B b) {
			this.b = b;
		}
	}

	static class B {
		public int val;

		public B(int val) {
			this.val = val;
		}
	}
	
	public static void main(String[] argv) {
		A a1 = new A(new B(input()));
		A a2 = new A(new B(input()));
		println(a1.b.val); // ok
		println(a2.b.val); // ok

		a2.b.val = secret();
		println(a1.b.val); // ok
		println(a2.b.val); // illegal

		a2.b = a1.b;
		println(a1.b.val); // ok
		println(a2.b.val); // ok

		a2.b.val = secret();
		println(a1.b.val); // illegal
		println(a2.b.val); // illegal
	}

	@Sink
	public static void println(int s) {}
	@Source
	public static int secret() { return 42; };
	public static int input() { return 23; };
	
}