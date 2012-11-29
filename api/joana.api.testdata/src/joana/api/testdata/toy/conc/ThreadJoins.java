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

class MartThread extends Thread {

	public void run() {
		if (Shared.x == 42) {
			System.out.println("aha!");
		}

	}
}

class ClauThread extends Thread {

	public void run() {
		Shared.x = 17;
	}
}

class Shared {
	static int x;
}

public class ThreadJoins {

	public static void main(String[] args) throws InterruptedException {
		Thread t1 = new MartThread();
		Thread t2 = new ClauThread();

		t1.start();
		t1.join();

		t2.start();



	}

}
