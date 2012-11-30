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
public class KillingDefValid {
	
	public static class A {
		public int i;
		
		public A(int i) {
			this.i = i;
		}
	}

	public static void main(String[] args) {
		A a = new A(Security.PUBLIC);
		a.i = Security.SECRET;
		a.i = Security.PUBLIC;
		Security.leak(a.i);				// ok
		int arr[] = new int[10];
		arr[5] = Security.SECRET;
		if (a.i > 123) {
			arr[5] = Security.PUBLIC;
		} else {
			arr[5] = Security.PUBLIC;
		}
		Security.leak(arr[5]);			// ok
	}

}
