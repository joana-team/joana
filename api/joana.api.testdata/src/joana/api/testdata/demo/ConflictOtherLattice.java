/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

/**
 * This class uses a non-standard lattice for IFC annotations:
 *
 *            H
 *            |
 *            |
 *            C
 *           / \
 *          /   \
 *         A     B
 *          \   /
 *           \ /
 *            L
 *
 * @author Simon Bischof <simon.bischof@kit.edu>
 */
public class ConflictOtherLattice {

	public static void main(String[] args) {
		int h = inputH();
		new Thread1().start();
		while (h > 0) {
			h--;
		}
		printA(1);
	}

	static class Thread1 extends Thread {
		public void run() {
			printB(0);
		}
	}

	@Sink(level="A")
	static void printA(int x) {}

	@Sink(level="B")
	static void printB(int x) {}

	@Source(level="H")
	static int inputH() {
		return 0;
	}

}
