/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.toy.demo;

public class Demo2 {

	public int generateSecret() {
		return 42;
	}

	public void leakage() {
		int x = generateSecret();
		System.out.println(x);
	}


	public static void main(String[] args) {
		Demo2 d = new Demo2();
		d.leakage();
	}


}
