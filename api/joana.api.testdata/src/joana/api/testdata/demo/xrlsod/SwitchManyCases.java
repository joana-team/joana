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

public class SwitchManyCases {
	@Source
	public static int HIGH;
	@Sink
	public static int LOW;

	public static class ThreadA extends Thread {
		public void run() {
			int tmp = HIGH;
			switch (tmp) {
				case 1: tmp--;break;
				case 2: tmp--;tmp--;break;
				case 3: tmp--;tmp--;tmp--;break;
			}
			LOW = 1;
		}
	}

	public static class ThreadB extends Thread {
		public void run() {
			LOW = 2;
		}
	}


	public static void main(String[] args) {
		Thread t1 = new ThreadA();
		Thread t2 = new ThreadB();
		t1.start();
		t2.start();
	}
}
