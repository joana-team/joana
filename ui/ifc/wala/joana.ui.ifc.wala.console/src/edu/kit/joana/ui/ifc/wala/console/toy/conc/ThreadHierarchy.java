/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.toy.conc;


class Thread1 extends Thread {
	public void run() {
		new Thread2().start();
	}
}

class Thread2 extends Thread {
	public void run() {
		Thread3 t1 = new Thread3();
		Thread3 t2 = new Thread3();

		t1.start();
		t2.start();
	}
}

class Thread3 extends Thread {
	public void run() {
		System.out.println("Hello, World!");
	}
}

public class ThreadHierarchy {

	public static void main(String[] args) {
		new Thread1().start();
	}
}
