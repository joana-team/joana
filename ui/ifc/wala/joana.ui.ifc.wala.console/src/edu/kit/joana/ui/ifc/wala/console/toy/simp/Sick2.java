/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.toy.simp;


class StaticConst {
	public static int modulus = 42;
}

public class Sick2 {

	private int count;

	public int foo(int a, int b) {
		this.count += 1;
		return (a + b) % StaticConst.modulus;
	}


	public static void main(String[] args) {
		Sick2 s = new Sick2();
		System.out.println(s.foo(17, 23));
	}
}
