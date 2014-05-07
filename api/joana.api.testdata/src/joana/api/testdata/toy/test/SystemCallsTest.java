/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.test;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.*;

public class SystemCallsTest {

	public static void main(String[] args) {
		int[] secretArray = {SECRET};
		int[] publicArray = new int[1];

		System.arraycopy(secretArray, 0, publicArray, 0, 1);
		leak(toggle(publicArray[0]));
	}
}
