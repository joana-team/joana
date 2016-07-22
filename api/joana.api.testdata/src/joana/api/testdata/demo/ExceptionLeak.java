package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class ExceptionLeak {

	static class A {
		public static A create(int i) {
			if (i != ExceptionLeak.secret())
				return new A();

			return null;
		}

		public void foo() {}
	}
	
	public static void main(String[] argv) {
		A a = A.create(input());

		try {
			a.foo();
			// illegal
			println("input != secret");
		} catch (NullPointerException exc) {
			// illegal
			println("input == secret");
		}
	}

	@Sink
	public static void println(String s) {}
	@Source
	public static int secret() { return 42; };
	public static int input() { return 23; };
	
}