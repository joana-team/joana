package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Level;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;


public class SequentialLeaks {

	static int x, y;
	
	public static void main(String[] argv) {
		x = inputPIN();
		if (x < 1234)
			print(0);
		y = x;
		y = 0;
		print(y);
	}

	@Source(level = Level.HIGH)
	public static int inputPIN() { return 42; }
	@Sink
	public static void print(int i) {}

}
