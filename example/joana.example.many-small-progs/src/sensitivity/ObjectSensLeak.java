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
public class ObjectSensLeak {
	
	public static class A {
		private int i;
		
		public A(int i) {
			this.i = i;
		}
		
		public void doPrint() {
			Security.leak(this.i);
		}
 	}

	public static void main(String[] args) {
		A a1 = new A(Security.PUBLIC);
		A a2 = new A(Security.SECRET);
		
		// ok
		a1.doPrint();
		// illegal
		a2.doPrint();
	}

}
