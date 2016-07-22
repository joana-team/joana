package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class InformationFlowLeaks {

	static int l, h;
	
	public static void main(String[] argv) {
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