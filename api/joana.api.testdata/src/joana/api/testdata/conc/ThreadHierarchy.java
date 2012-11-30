/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.conc;


/**
 * In this example, a simple thread hierarchy is created, which forms a complete binary tree of
 * height 3. Hence, there should be 15 threads. None of them is created within a loop or inside
 * a group of mutual recursive methods, so all of them should be non-dynamic.
 * @author Martin Mohr
 */
public class ThreadHierarchy {
	
	public static void main(String[] args) {
		Thread t1 = new Thread1();
		Thread t2 = new Thread1();
		t1.start();
		t2.start();
	}
}

class Thread1 extends Thread {
	public void run() {
		Thread t1 = new Thread2();
		Thread t2 = new Thread2();
		t1.start();
		t2.start();
	}
}

class Thread2 extends Thread {
	public void run() {
		Thread t1 = new Thread3();
		Thread t2 = new Thread3();
		t1.start();
		t2.start();
	}
}

class Thread3 extends Thread {
	public void run() {

	}
}
