/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.conc;


class RThread extends Thread {

	static String value = "I am secret.";
	static int[] secretNumbers = {42, 17, 23};


	public void run() {
		System.out.println(value);
		for (int i = 0; i < secretNumbers.length; i++) {
			System.out.println(value);
		}
	}
}

public class ThreadTestR {

	public static void main(String[] args) {
		new RThread().start();
		RThread.value = "I am tainted.";
		RThread.secretNumbers[2]++;
	}

}
