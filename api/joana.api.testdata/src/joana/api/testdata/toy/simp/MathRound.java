/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.simp;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.SECRET;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.leak;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.toggle;

/**
 * @author Simon Bischof <simon.bischof@kit.edu>
 */
public class MathRound {
	public static void main(String[] args) {
		/*
		 * i should not depend on the value of secret
		 * if toggle ignores its argument.
		 * However, JOANA assumes that Math.round can throw an exception,
		 * and so assumes that the execution of leak(...) depends on SECRET.
		 */
		int i = toggle(Math.round(SECRET));
		leak(i);
	}
}
