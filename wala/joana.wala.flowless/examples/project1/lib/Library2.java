/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package lib;


public class Library2 {

	public static class A {
		int i;
	}

	public static class B {
		A[] l;
	}

	public static class C {
		A a1;
		A a2;
		B b;
	}

	// Formerly expected error: No parameter with name 'b'
	// But now, most of the error checking is done later in the analysis.
	//@ ifc: !{a, b, c}
	public static int noSyntacticError1(A a, C c) {
		return a.i + c.a2.i;
	}

	// Formerly expected error: No parameter with name 'b'
	// But now, most of the error checking is done later in the analysis.
	//@ ifc: !{a, c} => b-!>\result
	public static int noSyntacticError2(A a, C c) {
		return a.i + c.a2.i;
	}

	// Expected error: Method has no return value, so '\result' does not exist
	//@ ifc: !{a, c.a1, c.a2} => a-!>\result
	public static void syntacticError3(A a, C c) {
	}

	// Formerly expected error: No field named 'l' of 'c.a2'
	// But now, most of the error checking is done later in the analysis.
	//@ ifc: !{a, c} => c.a1.l-!>\result
	public static int noSemanticError1(A a, C c) {
		return a.i + c.a2.i;
	}

	// Formerly expected error: No field named 'i' of '\result'
	// But now, most of the error checking is done later in the analysis.
	//@ ifc: !{a, c} => c.a1-!>\result.i
	public static int noSemanticError2(A a, C c) {
		return a.i + c.a2.i;
	}

	// Expected accept
	//@ ifc: !{a.i, z.l[].i, a.i, a.i} => x-!>\result
	public static int ok1(B z, A a, A b, A c, int x) {
		c.i = a.i;
		a.i = x;
		return b.i;
	}

	// Expected accept
	//@ ifc: !{a.i, b.i, c.i} => pure(b), a.i->c.i, pure(z), a.i-!> \result
	public static int ok2(B z, A a, A b, A c, int x) {
		c.i = a.i;
		a.i = x;
		return b.i;
	}

}
