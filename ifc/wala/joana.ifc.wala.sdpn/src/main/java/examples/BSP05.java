/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package examples;


public class BSP05 extends Thread{

	/**
	 * @param args
	 */
	static int x;
	static Lock lock1 = new Lock();

	public static void main(String[] args) {
		p1();
	}

	public void run() {
		p2();
	}


	static void p1() {
		BSP05 p2 = new BSP05();
		p2.start();
		synchronized(lock1) {
			killX();
			dummy(x);
		}
	}
	static void p2() {
		synchronized(lock1) {
			x = 42;
		}
	}

	public static void dummy(int y) {}

	public static void killX() {
		x = 0;
	}



	//TODO write-write Interferenzen? Output: Checking interference from 1@< Application, Lexamples/BSP05, p2()V > to 1@< Application, Lexamples/BSP05, killX()V >

}
