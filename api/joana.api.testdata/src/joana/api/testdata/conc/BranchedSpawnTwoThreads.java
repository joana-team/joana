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
public class BranchedSpawnTwoThreads {

	static class Thread2 extends Thread {
		public void run() {
			System.out.println("2");
		}
	}
	
	static class Thread3 extends Thread {
		public void run() {
			System.out.println("3");
		}
	}
	
	static int x;
	
	public static void main(String[] args) throws InterruptedException {
		Thread2 t2 = new Thread2();
		Thread3 t3 = new Thread3();
		

		if (x > 1) {
			t2.start();
		} else {
			t3.start();
		}
	}

}
