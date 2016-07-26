/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.conc;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class LotsOfDominationInMainThread {

	static class Thread1 extends Thread {
		public void run() {
			System.out.println("1");
		}
	}
	

	static int x,y;
	
	public static void main(String[] args) throws InterruptedException {
		Thread1 t1 = new Thread1();
	
		t1.start();
		x = 1;
		x = 2;
		x = 3; 
		x = 4;
		x = 5; 
		x = 6;
		x = 7;
		System.out.println("main");
	}
}
