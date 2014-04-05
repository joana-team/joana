/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

import sensitivity.Security;

public class ThreadSpawning {
	
	public static int shared;
	
	public static void main(String[] args) {
		T1 t1b = new T1();
		t1b.start();
		T2 t2 = new T2();
		t2.start();
		while (true) {
			T1 t1 = new T1();
			t1.start();
			Security.PUBLIC = shared;
		}
	}
}

class T1 extends Thread {
	public void run() {
		ThreadSpawning.shared = 23;
		if (Security.SECRET > 0 ) {
			Security.SECRET--;
		}
		T2 t2 = new T2();
			t2.start();
	}
}

class T2 extends Thread {
	public void run() { 
		ThreadSpawning.shared = 42;
	}
}
