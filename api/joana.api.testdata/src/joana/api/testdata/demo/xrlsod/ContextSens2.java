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

public class ContextSens2 {
	@Source
	static int HIGH;
	@Sink
	static int LOW;
	public static void main(String[] args) {
		HIGH = contextLossClassical(HIGH);
		int x = contextLossClassical(9);
		
		Thread t = new Thread1();
		t.start();
		contextLossTiming(3);
		LOW = x;
		contextLossTiming(HIGH);
	}
	
	static int contextLossClassical(int x) {
		return contextLossClassical2(x);
	}
	
	static int contextLossClassical2(int x) {
		return x;
	}
	
	static void contextLossTiming(int x) {
		contextLossTiming2(x);
	}
	
	static void contextLossTiming2(int x) {
		int y = x;
		while (y > 0) {
			y--;
		}
	}
	
	static class Thread1 extends Thread {
		public void run() {
			LOW = 3;
		}
	}
}
