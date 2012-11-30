/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package examples;


public class BSP04 extends Thread {
	static void p1() {
		thread.start();
	    synchronized(lock1){
	        synchronized(lock2) { dummy(); }
	        excludeMe();
	    }
	}

	void p2() {
		synchronized(lock2){
	        synchronized(lock1) { dummy(); }
	        excludeMe();
	    }
	}

	static Lock lock1 = new Lock();
	static Lock lock2 = new Lock();
	static Lock lock3 = new Lock();
	static BSP04 thread = new BSP04();


	public static void main(String[] args) {
		p1();
	}

	public void run() {
		p2();
	}

	static void excludeMe() {
	}

	static void dummy() {
	}


}
