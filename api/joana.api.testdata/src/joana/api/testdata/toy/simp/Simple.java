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
package joana.api.testdata.toy.simp;

public class Simple {

	private Object myAttribute;

	public int foo(int x) {
		int y = 0;
		Object o = null;
		while (y < 100) {
			o = new Object();
			foo(y);
			y++;
		}

		int z = y;

		while (z >= 0) {
			z--;
			o = new String();
		}

		return o.hashCode();
	}

	public void bar() {
		for (int i = 0; i < 100; i++) {
			foo(i);
		}
	}

	public void troodles(int x) {
		int y = 2 * x;
		int z = 4 * y - 3 * x;
	}

	public void cutchie(int x) {
		int y;
		y = x + x;
	}

	public void faraw() {
		myAttribute = new String();
	}

	public static void main(String[] args) {
		Simple s = new Simple();
		s.foo(100);
		s.bar();
		s.troodles(Integer.parseInt(args[0]));
		s.cutchie(Integer.parseInt(args[1]));
		s.faraw();
	}
}
