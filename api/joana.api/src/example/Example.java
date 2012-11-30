/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package example;

public class Example {

	private static Secret sec = new Secret(42);
	private static Public pub;


	public static void direct() {
		pub = new Public(sec.getValue());
	}


	public int foo(int x) {
		return x + 10;
	}

	public static void main(String[] args) {
		Example e = new Example();
		e.foo(42);
	}
}
