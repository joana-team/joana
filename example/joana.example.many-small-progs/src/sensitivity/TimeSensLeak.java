/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package sensitivity;


/**
 * @author Martin Mohr
 */
public class TimeSensLeak {

	public static class T extends Thread {
		public int x = Security.PUBLIC;
		public int y = Security.PUBLIC;
		
		public void run() {
			x = y;
		}
	}
	
	public static void main(String[] args) {
		T t = new T();
		t.start();
		t.y = Security.SECRET;
		int p = t.x;
		Security.leak(p);		// leak without time-travel possible!
	}

}

