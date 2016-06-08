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
public class MutualRecursiveSpawn {
	static class Thread1a extends Thread {
		public void run() {
			System.out.println("1a");
			new Thread1b().start();
		}
	}
	
	static class Thread1b extends Thread {
		public void run() {
			System.out.println("1b");
			new Thread1a().start();
		}
	}
	
	static class Thread2a extends Thread {
		public void run() {
			System.out.println("2a");
			new Thread2b().start();
		}
	}
	
	static class Thread2b extends Thread {
		public void run() {
			new Thread2a().start();
			System.out.println("2b");
		}
	}
	
	static class Thread3a extends Thread {
		public void run() {
			new Thread3b().start();
			System.out.println("3a");
		}
	}
	
	static class Thread3b extends Thread {
		public void run() {
			System.out.println("3b");
			new Thread3a().start();
		}
	}
	
	static class Thread4a extends Thread {
		public void run() {
			new Thread4b().start();
			System.out.println("4a");
		}
	}
	
	static class Thread4b extends Thread {
		public void run() {
			new Thread4a().start();
			System.out.println("4b");
		}
	}
	
	static int x;
	
	public static void main(String[] args) throws InterruptedException {
		new Thread1a().start();
		new Thread2a().start();
		new Thread3a().start();
		new Thread4a().start();
	}
}