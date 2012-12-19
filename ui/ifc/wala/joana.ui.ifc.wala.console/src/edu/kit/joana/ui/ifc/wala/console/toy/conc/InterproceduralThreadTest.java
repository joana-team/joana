/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.toy.conc;

class SomeThreadyThing extends Thread {

	public void run() {
		System.out.println("Another thread has emerged! Hail to all threads!");
	}
}

class ThreadStarter {

	void doIt() {
		SomeThreadyThing t = new SomeThreadyThing();
		Thread t2 = f1(t);
		Thread t3 = f2(t2);
		t3.start();
	}

	Thread f1(Thread t) {
		return t;
	}

	Thread f2(Thread t) {
		return t;
	}
}

public class InterproceduralThreadTest {



	public static void main(String[] args) {
		new ThreadStarter().doIt();
	}
}
