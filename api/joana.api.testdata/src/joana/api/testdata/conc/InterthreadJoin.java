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
public class InterthreadJoin {
	static class Thread1a extends Thread {
		public void run() {
			t1c = new Thread1c();
			t1c.start();
			System.out.println("1a");
		}
	}
	
	static class Thread1b extends Thread {
		public void run() {
			try {
				t1a.join();
			} catch (InterruptedException e) {
			}
			System.out.println("1b");
		}
	}
	static class Thread1c extends Thread {
		public void run() {
			System.out.println("1c");
		}
	}
	
	static class Thread2a extends Thread {
		public void run() {
			try {
				t2b.join();
			} catch (InterruptedException e) {
			}
			System.out.println("2a");
		}
	}
	
	static class Thread2b extends Thread {
		public void run() {
			System.out.println("2b");
		}
	}
	
	static Thread1a t1a;
	static Thread1b t1b;
	static Thread1c t1c;
	static Thread2a t2a;
	static Thread2b t2b;
	
	public static void main(String[] args) throws InterruptedException {
		t1a = new Thread1a();
		t1a.start();
		t1b = new Thread1b();
		t1b.start();
		t2a = new Thread2a();
		t2a.start();
		t2b = new Thread2b();
		t2b.start();
	}
}