package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class Fig2_1 {

	static int l, h;
	
	public static void main(String[] argv) throws InterruptedException {
		h = inputPIN();
		if (h < 1234)
			print(0);
		l = h;
		print(l);
	}

	@Source
	public static int inputPIN() { return 42; }
	@Sink
	public static void print(int s) {}
	public static int input() { return 13; }
	
}