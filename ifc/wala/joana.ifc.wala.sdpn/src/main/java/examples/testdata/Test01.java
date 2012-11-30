/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package examples.testdata;

public class Test01 extends Thread {
	int x = 0;
	public Test01() {
		synchronized(this) {
			x = 42;
		}
	}

	static Test01 other = null;

	public static void main(String[] args) {
		Test01 a = new Test01();
		a.start();
		synchronized(a) {
			other = new Test01();
		}
	}

	public void run() {
		synchronized(this){
			System.out.println(other.x);
		}
	}
}
