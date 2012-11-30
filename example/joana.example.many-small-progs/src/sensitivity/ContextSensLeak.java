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
public class ContextSensLeak {
	
	public static class A {
		public static void doPrint(int i) {
			Security.leak(i);
		}
	}

	public static void main(String[] args) {
		A.doPrint(Security.PUBLIC);		// ok
		A.doPrint(Security.SECRET);		// illegal
	}

}
