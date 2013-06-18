/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package examples.testdata;

public class Killing02 extends Thread {
	static int x = 0;

	public static void main(String[] args) {
		Killing02 a = new Killing02();
		a.start();
		synchronized(a){
			killX();
			print(x);
		}
	}

	public static void killX(){
		x = 0;
	}

	public void run() {
		synchronized(this){
			x = 42;
		}
	}
	
	static void print(int x){
		// this is the sink
	}
}
