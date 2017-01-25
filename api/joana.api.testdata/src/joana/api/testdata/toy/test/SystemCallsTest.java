/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.test;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.SECRET;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.leak;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.toggle;

public class SystemCallsTest {

	public static void main(String[] args) {
		// in this case, we write a secret value into the array,
		// but toggle it before leaking
		int[] secretArray = {SECRET};
		int[] publicArray = new int[1];

		System.arraycopy(secretArray, 0, publicArray, 0, 1);
		leak(toggle(publicArray[0]));

		// in this case, we toggle the value before writing it into the array
		int[] secretArray2 = {toggle(SECRET)};
		int[] publicArray2 = new int[1];

		System.arraycopy(secretArray2, 0, publicArray2, 0, 1);
		leak(publicArray2[0]);
	}
}
