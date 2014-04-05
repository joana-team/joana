/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

import sensitivity.Security;

public class ThreadJoining {
	public static void main(String[] args) throws InterruptedException {
		A a = new A();
		a.start();
		a.join();
		Security.leak(a.x);
		System.out.println(a.x);
	}
}

class A extends Thread {
    int x = Security.SECRET;
	public void run() {
	    while (x > 0) {
	        x--;
	    }
	}
}
