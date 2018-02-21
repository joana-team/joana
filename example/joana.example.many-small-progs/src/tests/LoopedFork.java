/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

import sensitivity.Security;

public class LoopedFork {
	static T t;
	
	public static void main(String[] args) {
		
		t = new T(10);
		t.foo();
		
		Security.leak(42);
	}
	
	static class T extends Thread {
		int x;

		void foo() {
			start();
		}
		
		T(int x) {
			this.x = x;
		}
		
		@Override
		public void run() {
			foo();
		}
	}
}
