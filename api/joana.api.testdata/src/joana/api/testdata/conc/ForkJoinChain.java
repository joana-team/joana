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
public class ForkJoinChain {
	
	static class Thread1 extends Thread {
		public void run() {
			Thread2 t = new Thread2();
			t.start();
			try {
				t.join();
			} catch (InterruptedException e) {
			}
			System.out.println("1");
		}
	}
	
	static class Thread2 extends Thread {
		public void run() {
			Thread3 t = new Thread3();
			t.start();
			try {
				t.join();
			} catch (InterruptedException e) {
			}
			System.out.println("2");
		}
	}
	
	static class Thread3 extends Thread {
		public void run() {
			System.out.println("3");
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		Thread1 t1 = new Thread1();
		t1.start();
		t1.join();
		System.out.println("Main");
	}
}
