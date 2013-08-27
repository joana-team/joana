/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.conc;

/**
 * TODO: @author Add your name here.
 */
public class HuismanExample {

	static int h;
	static int l1;
	static int l2;

	public static void main(String[] args) {
		Thread1 t1 = new Thread1();
		Thread2 t2 = new Thread2();
		Thread3 t3 = new Thread3();

		t1.start();
		t2.start();
		t3.start();

	}

	static class Thread1 extends Thread {
		public void run() {
			if (h > 0) {
				l1 = 1;
			} else {
				l2 = 1;
			}
		}
	}

	static class Thread2 extends Thread {
		public void run() {
			l1 = 1;
			l2 = 1;
		}
	}

	static class Thread3 extends Thread {
		public void run() {
			l2 = 1;
			l1 = 1;
		}
	}
}
