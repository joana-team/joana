/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.conc;

/**
 * @author Martin Mohr
 */
public class DataConflictRWBenign {
	
	static class Thread1 extends Thread {
		public void run() {
			x = 1;
			System.out.println("You say goodbye");
		}

	}
	
	static class Thread2 extends Thread {
		public void run() {
			if (x == 1) {
				x--;
			}
			System.out.println("and I say hello");
		}
	}
	
	private static int x;
	
	public static void main(String[] args) {
		Thread1 t1 = new Thread1();
		Thread2 t2 = new Thread2();
		
		t1.start();
		t2.start();
	}
	
	
	
}
