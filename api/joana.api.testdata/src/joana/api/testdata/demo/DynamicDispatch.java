package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class DynamicDispatch {

	static class A {
		public boolean foo() {
			return false;
		}
	}

	static class B extends A {
		public boolean foo() {
			return true;
		}
	}
	
	public static void main(String[] argv) {
		A a = new A();
		if (input() == secret()) {
			a = new B();
		}
		if (a.foo()) {
			// illegal
			println("input == secret");
		} else {
			// illegal
			println("input != secret");
		}
	}

	@Sink
	public static void println(String s) {}
	@Source
	public static int secret() { return 42; };
	public static int input() { return 23; };
	
}