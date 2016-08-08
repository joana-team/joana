/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.demo.xrlsod;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

/*
 * This program is RLSOD because the order of "print(l)" and "l = 4"
 * does not depend on high timing.
 * While the conflict of "l = h" and "l = 4" can be influenced by high timing,
 * this conflict is not visible because the print statement executes before "l = h".
 * Currently, the timingiRLSOD analysis is time insensitive and reports a leak here.
 */
public class TimeSens {

	static int l, h;
	
	public static void main(String[] argv) throws InterruptedException {
		new Thread_1().start();
		print(l);
		while (h > 0) {
			h--;
		}
		l = h;
	}

	static class Thread_1 extends Thread {
		public void run() {
			l = 4;
			h = inputPIN();
		}
	}
	
	@Source
	public static int inputPIN() { return 42; }
	@Sink
	public static void print(int i) {}

}
