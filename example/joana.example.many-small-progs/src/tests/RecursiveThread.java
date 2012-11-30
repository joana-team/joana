/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

public class RecursiveThread {
	public static void main(String[] args) {
		T t = new T(10);
		t.start();
	}
}

class T extends Thread {
	int x;

	T(int x) {
		this.x = x;
	}

	public void run() {
		if (x > 0) {
			x--;
			foo();
		}
	}

	void foo() {
    	T t = new T(x);
        t.start();
        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println(x);
        }
	}
}
