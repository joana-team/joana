/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.conc;

/**
 * In this example, a thread is spawned, which spawns another thread only if some condition is
 * satisfied. Analysis should conservatively assume, that this other thread is always spawned,
 * but should nevertheless identify it as non-dynamic.
 * @author Martin Mohr
 */
public class ConditionalSpawn {
	
	public static void main(String[] args) {
		Thread tCond = new ThreadWithConditionalSpawn(42);
		tCond.start();
	}
}

class ThreadWithConditionalSpawn extends Thread {

	private int n;
	
	public ThreadWithConditionalSpawn(int n) {
		this.n = n;
	}
	
	public void run() {
		if (n > 42) {
			Thread t = new SimpleThread();
			t.start();
		}
	}
	
}
