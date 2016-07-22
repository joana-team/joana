package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class ExcTest {

	static int l, h;
	
	static class A {
		int i;
	}
	
	public static void main(String[] argv) throws InterruptedException {
		h = inputPIN();
		A a = new A();
		if (h < 1234)
			a = null;
//		if (a != null)
			a.i = 23;
		print(l);
	}

	@Source
	public static int inputPIN() { return 42; }
	@Sink
	public static void print(int s) {}
	public static int input() { return 13; }
	
}
