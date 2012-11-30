/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package lib;

/**
 * This is the first example from the very first talk that lead to the
 * DFG proposal.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class Library {

	public static class A {
		int i;
		int f;
	}

	int i;

//	//@ ifc: => (*, this, this.i)-!>\result
//	int getI() {
//		this.i = 3;
//		return 4;
//	}
	// ifc: !{a, b} => x-!>\result, c.i->\result, a.i-!>b.i

	//@ ifc: !{a, b} => x-!>\result
	public static int call(A a, A b, A c, int x) {
		c.i = a.i;
		a.i = x;
		return b.i;
	}

//	//@ ifc: !{a, b} => x-!>\result
//	public static int callIndirect(A a, A b, A c, int x) {
//		c.i = a.i;
//		if (x > 4) {
//			a.i = 2;
//		}
//		return b.i;
//	}
//
//	//@ ifc: !{a, c, b} => x-!>\result
//	public static int callIndirect2(A a, A b, A c, int x) {
//		if (x > 4) {
//			c.i = 2;
//		}
//
//		a.i = c.i;
//
//		return b.i;
//	}
//
//	//@ ifc: !{a, b} => a-!>\result
//	public static int call2(A a, A b) {
//		A c = new A();
//		c.i = a.i;
//		return return2(c, b);
//	}
//
//	public static int return2(A a, A b) {
//		return b.i;
//	}
//
//	//@ ifc: => (*)-!>\state
//	//@ ifc: => a-!>(\result, \result.f, \result.i, \result.*)
//	public static A call3(A a, A b) {
//		if (a == null) {
//			return b;
//		}
//
//		A ret = (true ? new A() : a);
//		ret.i = b.i;
//
//		return ret;
//	}
}
