/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.conc;

/**
 * @author Simon Bischof <simon.bischof@kit.edu>
 */
public class ForkJoin {
	static class Thread1 extends Thread {
		public void run() {
			System.out.println("1");
		}
	}

	static Thread1 t1 = new Thread1();
	
	public static void main(String[] args) throws InterruptedException {
		t1.join();
		t1.start();
		System.out.println("Main");
	}
}