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
package edu.kit.joana.ui.ifc.wala.console.toy.simp;

class Secret {
	private int sec;

	public Secret(int sec) {
		this.sec = sec;
	}

	public int getSecret() {
		return sec;
	}
}

class Foo {
	Bar a;
	Foo() {
		a = new Bar();
	}
}

class Bar {
	Bar() {
		b = new Troodles();
	}
	Troodles b;
}

class Troodles {

	int somethingFancy(int u) {
		return 2 * u + 1;
	}
}

public class AssChain {

	public static void main(String[] args) {
		Secret s = new Secret(17);
		Foo x = new Foo();
		Bar y = x.a;
		Troodles z = y.b;
		System.out.println(z.somethingFancy(s.getSecret()));
	}
}
