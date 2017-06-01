/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.pruning.test1.library;

/**
 * TODO: @author Add your name here.
 */
public class Library {
	
	public static int combineAllWrapper(CallbackInterface cb, int a, int b) {
		return combineAllWrapper2(cb, a, b);
	}
	
	public static int combineAllWrapper2(CallbackInterface cb, int a, int b) {
		return combineAllWrapper3(cb, a, b);
	}
	
	public static int combineAllWrapper3(CallbackInterface cb, int a, int b) {
		return combineAllWrapper4(cb, a, b);
	}
	
	public static int combineAllWrapper4(CallbackInterface cb, int a, int b) {
		return combineAllWrapper5(cb, a, b);
	}
	
	public static int combineAllWrapper5(CallbackInterface cb, int a, int b) {
		return combineAllWrapper6(cb, a, b);
	}

	public static int combineAllWrapper6(CallbackInterface cb, int a, int b) {
		return combineAllWrapper7(cb, a, b);
	}

	public static int combineAllWrapper7(CallbackInterface cb, int a, int b) {
		return combineAllWrapper8(cb, a, b);
	}

	public static int combineAllWrapper8(CallbackInterface cb, int a, int b) {
		return combineAllWrapper9(cb, a, b);
	}

	public static int combineAllWrapper9(CallbackInterface cb, int a, int b) {
		return combineAllWrapper10(cb, a, b);
	}

	
	public static int combineAllWrapper10(CallbackInterface cb, int a, int b) {
		return combineAll(cb, a, b);
	}

	public static int combineAll(CallbackInterface cb, int a, int b) {
		int x = cb.combine(a, cb.combine(b, cb.getNeutral()));
		return x;
	}

	
}
