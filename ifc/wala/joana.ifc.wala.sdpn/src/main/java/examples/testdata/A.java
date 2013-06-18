/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package examples.testdata;

public class A extends Thread {
	static int x = 0;

	public static void main(String[] args) {
		A a = new A();
		synchronized(a){
			a.start();
			print(x);
		}
	}

	public void run() {
		synchronized(this){
			print(x);
			x = 17;
		}
		x = 42;
	}
	
	static void print(int x){
		// this is the sink
	}
}

