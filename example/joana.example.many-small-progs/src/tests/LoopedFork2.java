/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

import sensitivity.Security;
import tests.LoopedFork.T;

public class LoopedFork2 {
	static Thread t;
	static Thread t1;
	static Thread t2;
	
	public static void main(String[] args) {
		
		if (args.length > 0) {
			t1 = new T(10);
			t = t1;
		} else {
			t2 = new T2();
			t = t2;
		}
		rec(5);
		Security.leak(42);
	}
	
	static void rec(int n) {
		t.start();
		rec2(n);
	}
	
	static void rec2(int n) {
		rec(n);
	}
	
	static class T extends Thread {
		int x;

		T(int x) {
			this.x = x;
		}
		
		@Override
		public void run() {
			this.start();
		}
	}
	
	static class T2 extends Thread {
		@Override
		public void run() {
			rec(5);
		}
	}
}
