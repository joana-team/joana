/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

public class IndirectRecursiveThreads {
	public static void main(String[] args) {
		Ta t1b = new Ta();
		t1b.start();
		Tb t2 = new Tb();
		t2.start();
		while (true) {
			Ta t1 = new Ta();
			t1.start();
		}
	}
}

class Ta extends Thread {
	public void run() {
		Tb t2 = new Tb();
		t2.start();
	}
}

class Tb extends Thread {
	public void run() {
		Tb t1 = new Tb();
		t1.start();
	}
}
