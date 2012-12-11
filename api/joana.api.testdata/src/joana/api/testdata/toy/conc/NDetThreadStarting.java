/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.conc;

class AThread extends Thread {
	public void run() {
		NDetThreadStarting.x = 0;
	}
}

class BThread extends Thread {
	public void run() {
		NDetThreadStarting.x = 1;
	}
}

public class NDetThreadStarting {

	static int x;

	public static void main(String[] args) throws InterruptedException {
		Thread t = new AThread();
		t.start();
		t.join();
		x = 1;
		if (x == 0) {
			System.out.println("hu");
		} else {
			System.out.println("ha");
		}
	}
}
