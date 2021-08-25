/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.simp;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.SECRET;

public class TinyExample2 {

	public static int func(int x, int y) {
		if (x == 0) {
			return y;
		}
		return 0;
	}

	public static void main(String[] args) {
		func(SECRET, SECRET);
	}
}
