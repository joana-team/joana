package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class ExceptionTest {
	@Source
	public static boolean SECRET;
	public static int UNCRITICAL;
	@Sink
	public static int PUBLIC = 0;

	private static class A {
		int x;
	}

	public static void foo(A a1, A a2) {
		if (a2.x > 10) {
			UNCRITICAL = 1;
		} else {
			UNCRITICAL = 2;
		}
	}

	public static void main(String[] args) {
		if (SECRET) {
			foo(new A(), new A());
		} else {
			m();
		}
		PUBLIC = 17;
	}

	private static void m() {
		foo(new A(), null);
	}

}
