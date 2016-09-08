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
public class OtherThreadJoinsIndirect {
	
	static class Thread1 extends Thread {
		public void run() {
			try {
				t3.join();
			} catch (InterruptedException e) {
			}
			System.out.println("1");
		}
	}
	
	static class Thread2 extends Thread {
		public void run() {
			t3.start();
		}
	}
	
	static class Thread3 extends Thread {
		public void run() {
			System.out.println("3");
		}
	}
	
	static Thread3 t3 = new Thread3();
	
	public static void main(String[] args) throws InterruptedException {
		new Thread2().start();
		new Thread1().start();
	}
}
