/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

public class VolpanoSmith98Page3 {
	static class Alpha extends Thread {
		public void run() {
			while (mask != 0) {
				while (trigger0 == 0) { /* busy wait */ }
				System.out.println("ALPHA");
				result = result | mask;
				trigger0 = 0;
				maintrigger++;

				if (maintrigger == 1) {
					trigger1 = 1;
				}
			}
		}
	}

	static class Beta extends Thread {
		public void run() {
			while (mask != 0) {
				while (trigger1 == 0) { /* busy wait */ }
				System.out.println("BETA");
				result = result & ~mask;
				trigger1 = 0;
				maintrigger++;

				if (maintrigger == 1) {
					trigger0 = 1;
				}
			}
		}
	}

	static class Gamma extends Thread {
		public void run() {
			while (mask != 0) {System.out.println("mask: "+mask);
				maintrigger = 0;

				if ((PIN & mask) == 0) {
					trigger0 = 1;
				} else {
					trigger1 = 1;
				}

				while (maintrigger < 2) { /* busy wait */ }

				mask = mask / 2;
			}

		}
	}

	static int maintrigger = 0;
	static int trigger0 = 0;
	static int trigger1 = 0;
	static int mask = 2048; // a power of 2
	static int PIN = 0;     // less twice mask
	static int result = 0;

	public static void main(String[] args) throws Exception {
		PIN = Integer.parseInt(args[0]);
		Thread a = new Alpha();
		Thread b = new Beta();
		Thread g = new Gamma();

		g.start();
		a.start();
		b.start();

		g.join();
		a.join();
		b.join();

		System.out.println(result);
	}
}
