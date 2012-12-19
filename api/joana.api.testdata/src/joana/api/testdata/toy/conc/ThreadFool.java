/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.conc;

class FoolThread extends Thread {

	public void run() {

	}
}

public class ThreadFool {

	public void execute() {
		FoolThread t = new FoolThread();
		Thread t2 = bar(t);
		fool(t2);
	}

	public Thread bar(Thread t) {
		return t;
	}

	public void fool(Thread t) {
		t.start();
	}

	public static void main(String[] args) {
		new ThreadFool().execute();
	}
}
