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


class ThreadWrapper extends Thread {

	private Thread t;

	public ThreadWrapper(Thread t) {
		this.t = t;
	}

	public void run() {
		t.start();
	}
}

public class ThreadHierarchy2 {


	public static void main(String[] args) {
		Thread t1 = new Thread();
		Thread t2 = new ThreadWrapper(t1);
		Thread t3 = new ThreadWrapper(t2);
		t3.start();
	}
}
