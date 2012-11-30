/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package examples.testdata;

public class Killing04 extends Thread {
	static int x = 0;

	public static void main(String[] args) {
		Killing04 a = new Killing04();
		a.start();
		synchronized(a){
			System.out.println(x);
		}
	}

	public void run() {
		synchronized(this){
			x = 42;
		}
	}
}
