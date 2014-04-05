/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package exc;

import sensitivity.Security;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ExceptionOptimize {

	public ExceptionOptimize() {}

	public static void main(String[] argv) {
		ExceptionOptimize opt = new ExceptionOptimize();
		opt.simpleTests(new A());
	}

	public int simpleTests(A p) {
		A a = new A();
		if (Security.SECRET > 2) {
			a.i = 4;		// NEVER Exc
		}
		A b = new A();
		if (Security.SECRET > 2) {
			b.i = p.i;		// NEVER Exc (interproc)
		}
		A c = p;
		if (p != null) {
			if (Security.SECRET > 2) {
				c.i = 4;	// NEVER Exc
			}
		} else {
			if (Security.SECRET > 2) {
				c = b;
				p.i = 13;	// ALWAYS Exc
			}
		}

		if (Security.SECRET > 2) {
			c.i = b.i;		// MAYBE Exc (c maybe null)
		}

		if (Security.SECRET > 2) {
			a.i = a.i + b.i + c.i; // NEVER Exc
		}
		
		if (Security.SECRET > 2) {
			b.x = 23;
		}
		
		Security.leak(23);
		
		return b.x;
	}

}
