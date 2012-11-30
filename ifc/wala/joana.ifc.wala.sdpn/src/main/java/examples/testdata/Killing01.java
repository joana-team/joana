/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package examples.testdata;

public class Killing01 extends Thread {
	static int x = 0;

	public static void main(String[] args) {
		Killing01 a = new Killing01();
		a.start();
		synchronized(a){
			x = 0;
			System.out.println(x);
		}
	}

	public void run() {
		synchronized(this){
			x = 42;
		}
	}
}
