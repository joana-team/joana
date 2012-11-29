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
package edu.kit.joana.ui.ifc.wala.console.toy.declass;

class SecretWrapper extends Secret {
	private Secret wrapped;

	public SecretWrapper(Secret wrapped) {
		this.wrapped = wrapped;
	}
}

public class Declass1 {

	static Object pub;

	public static Secret declass(Secret s) {
		return new SecretWrapper(s);
	}

	public static void bar(Object o) {
		pub = o;
	}

	public static void main(String[] args) {
		Secret s = new Secret();
		Secret s2 = declass(s);
		bar(s2);
	}
}
