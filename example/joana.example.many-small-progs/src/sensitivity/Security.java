/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package sensitivity;

/**
 * @author Juergen Graf <graf@kit.edu>
 */
public final class Security {

	public static int SECRET = 13;
	public static int PUBLIC = 21;

	public static void leak(int i) {
		PUBLIC += i;
	}
	
	public static void influence(int i) {
		SECRET += i;
	}
	
}
