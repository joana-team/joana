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
	
	static class Thread2 extends Thread {
		public void run() {
			System.out.println("2");
		}
	}

	static Thread1 t1 = new Thread1();
	static Thread2 t2;
	
	public static void main(String[] args) throws InterruptedException {
		t1.join();
		t1.start();
		startThread2_();
		startThread2_();
		t2.join();
		System.out.println("Main");
	}
	
	private static void startThread2_() {
		startThread2();
	}

	static void startThread2() {
		t2 = new Thread2();
		t2.start();
	}
}