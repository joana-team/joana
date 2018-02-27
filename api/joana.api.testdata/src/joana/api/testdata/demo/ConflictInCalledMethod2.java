/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.demo;

/**
 * This test case has a source in a called method.
 * 
 * @author Simon Bischof <simon.bischof@kit.edu>
 *
 */
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class ConflictInCalledMethod2 {

	static int l;

	public static void main(String[] argv) throws InterruptedException {
		new Thread_1().start();
		int x = f();
		print(x);
	}

	static int f() {
		return l;
	}

	static class Thread_1 extends Thread {
		public void run() {
			int h = inputPIN();
			while (h > 0) {
				h--;
			}
			l = 1;
		}
	}

	@Source
	public static int inputPIN() { return 42; }
	@Sink
	public static void print(int s) {}
	public static int input() { return 13; }

}