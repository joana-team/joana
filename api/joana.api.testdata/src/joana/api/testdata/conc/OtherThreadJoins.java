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
public class OtherThreadJoins {
	
	static class Thread1 extends Thread {
		public void run() {
			try {
				t2.join();
			} catch (InterruptedException e) {
			}
			System.out.println("1");
		}
	}
	
	static class Thread2 extends Thread {
		public void run() {
			System.out.println("2");
		}
	}
	
	static class Thread3 extends Thread {
		public void run() {
			try {
				t4.join();
			} catch (InterruptedException e) {
			}
			System.out.println("3");
		}
	}
	
	static class Thread4 extends Thread {
		public void run() {
			System.out.println("4");
		}
	}
	
	static class Thread5 extends Thread {
		public void run() {
			try {
				t6.join();
			} catch (InterruptedException e) {
			}
		}
	}
	
	static class Thread6 extends Thread {
		public void run() {
			System.out.println("6");
		}
	}

	static Thread2 t2 = new Thread2();
	static Thread4 t4 = new Thread4();
	static Thread6 t6 = new Thread6();
	
	public static void main(String[] args) throws InterruptedException {
		new Thread1().start();
		t2.start();
		
		t4.start();
		new Thread3().start();
		
		t6.start();
		Thread5 t5 = new Thread5();
		t5.start();
		t5.join();
		System.out.println("Main");
	}
}
