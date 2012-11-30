/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
import static java.lang.System.out;
/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class A {

	int i;

	public static void modify(A p, int x) {
		p.i = x;
	}

	public static void main(String argv[]) {
		A a = new A();
		A b = new A();
		modify(a, 4);
		modify(b, 6);
		int i = a.i;
		if (a.i != b.i) {
			modify(a, 8);
			int k = a.i;
			out.printf("i: %d, k: %d\n", i, k);
		}
	}

}
