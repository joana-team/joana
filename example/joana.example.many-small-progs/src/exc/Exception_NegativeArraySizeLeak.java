/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package exc;

import sensitivity.Security;

public class Exception_NegativeArraySizeLeak {

	public static void main(String[] args) throws Exception {
		int res;
		try {
			int a[] = new int[Security.SECRET];
			res = 1;
		} catch (NegativeArraySizeException e) {
			res = 0;
		}
		Security.leak(res);
	}
}
