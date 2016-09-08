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
public class IndirectSpawnJoin {
	
	static class Thread1 extends Thread {
		public void run() {
			t2.start();
		}
	}
	
	static class Thread2 extends Thread {
		public void run() {
			System.out.println("2");
		}
	}
	
	static class Thread3 extends Thread {
		public void run() {
			t4.start();
		}
	}
	
	static class Thread4 extends Thread {
		public void run() {
			System.out.println("4");
		}
	}

	static Thread2 t2 = new Thread2();
	static Thread4 t4 = new Thread4();
	
	public static void main(String[] args) throws InterruptedException {
		Thread1 t1 = new Thread1();
		t1.start();
		t1.join();
		t2.join();

		Thread3 t3 = new Thread3();
		t3.start();
		t4.join();
		System.out.println("Main");
	}
}
