/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package examples.testdata;

public class Killing03 extends Thread {
	static int x = 0;

	public static void main(String[] args) {
		Killing03 a = new Killing03();
		a.start();
		synchronized(a){
			System.out.println(x);
		}
	}

	public static void killX(){
		x = 0;
	}

	public void run() {
		synchronized(this){
			x = 42;
			killX();
		}
	}
}
