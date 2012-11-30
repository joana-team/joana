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
public class FieldSensValid {
	
	public static class A {
		public int i1;
		public int i2;
		
		public A(int i1, int i2) {
			this.i1 = i1;
			this.i2 = i2;
		}
	}

	public static void main(String[] args) {
		A a = new A(Security.PUBLIC, Security.SECRET);
		// ok
		Security.leak(a.i1);
		// illegal
//		Security.leak(a.i2);
	}

}
