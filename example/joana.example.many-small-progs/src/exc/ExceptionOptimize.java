/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package exc;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ExceptionOptimize {

	public ExceptionOptimize() {}

	public static void main(String[] argv) {
		ExceptionOptimize opt = new ExceptionOptimize();
		opt.simpleTests(null);
	}

	public int simpleTests(A p) {
		A a = new A();
		a.i = 4;		// NEVER Exc
		A b = new A();
		A c = p;
		if (p != null) {
			c.i = 4;	// NEVER Exc
		} else {
			c = b;
			p.i = 13;	// ALWAYS Exc
		}

		c.i = b.i;		// MAYBE Exc (c maybe null)

		return a.i + b.i + c.i; // NEVER Exc
	}

}
