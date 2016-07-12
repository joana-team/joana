package joana.api.testdata.demo;


import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class Fig3_1 {

	static int l, h, l1, h1;
	
	public static void main(String[] argv) throws InterruptedException {
		h = inputPIN();
		l = 2;
		h1 = f(h);
		l1 = f(l);
		print(l1);
	}

	private static int f(int x) {
		return x + 42;
	}
	
	@Source
	public static int inputPIN() { return 42; }
	@Sink
	public static void print(int s) {}
	public static int input() { return 13; }
	
}