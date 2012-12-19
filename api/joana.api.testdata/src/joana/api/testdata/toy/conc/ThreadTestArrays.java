/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.conc;

class ArrayThread extends Thread {

	static int[] secretNumbers = {42, 17, 23};


	public void run() {
		for (int i = 0; i < secretNumbers.length; i++) {
			System.out.println(secretNumbers[i]);
		}
	}
}

public class ThreadTestArrays {
	public static void main(String[] args) {
		new ArrayThread().start();
		ArrayThread.secretNumbers[2]++;
	}
}
