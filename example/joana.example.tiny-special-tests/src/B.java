/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class B {

	public int myb = 42;

	public static int i;
	public static int i2 = 33;

	static {
		i = 21 * i2;
	}

	public static void main(String[] args) {
		B b = new B();
		System.out.print(b.myb);
		System.out.print(B.i);
		B.i2 = B.i2 + B.i;
		System.out.print(B.i2);
	}

}
