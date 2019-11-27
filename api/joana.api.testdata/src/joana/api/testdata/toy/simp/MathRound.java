/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.simp;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.SECRET;

/**
 * @author Simon Bischof <simon.bischof@kit.edu>
 */
public class MathRound {

	public static void main(String[] args) {
		int i = Math.round(SECRET);
	}
}
