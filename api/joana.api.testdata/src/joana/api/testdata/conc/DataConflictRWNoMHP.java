/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.conc;

/**
 * @author Martin Hecker
 */
public class DataConflictRWNoMHP {
	
	
	static class Thread2 extends Thread {
		public void run() {
			System.out.println(x);
		}
	}
	
	static int x;
	
	public static void main(String[] args) throws InterruptedException {
		x = 1;
		Thread2 t2 = new Thread2();
		
		t2.start();
	}
	
	
	
}
