/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

public class IndirectSideEffects {

	public static class A1 {
		A2 a2;
	}

	public static class A2 {
		A3 a3;
	}

	public static class A3 {
		int i;
	}

	//@ifc: ? => i -!> p1.*
	public static void changeField(A1 p1, A3 p2, int i) {
		p2.i = i;
	}

	//@ifc: ? => p1.* -!> \result
	public static int readField(A1 p1, A3 p2) {
		return p2.i;
	}

}
