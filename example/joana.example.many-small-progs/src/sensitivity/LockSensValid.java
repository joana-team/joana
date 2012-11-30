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
public class LockSensValid {
	
	public static final Object l = new Object();

	public static class T extends Thread {
		
		public int y = Security.PUBLIC;
		
		public void run() {
			synchronized (LockSensValid.l) {
				y = Security.SECRET;
			}
		}
	}
	
	public static void main(String[] args) {
		T t = new T();
		synchronized (LockSensValid.l) {
			t.start();
			Security.leak(t.y);		// ok
		}
	}

}
