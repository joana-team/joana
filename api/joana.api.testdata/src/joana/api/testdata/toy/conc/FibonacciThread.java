/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.conc;

public class FibonacciThread extends Thread {

	private int n;
	private int result;

	public FibonacciThread(int n) {
		this.n = n;
	}

	public void run() {
		if (n <= 1) {
			this.result = n;
		} else {
			FibonacciThread tnm1 = new FibonacciThread(n - 1);
			FibonacciThread tnm2 = new FibonacciThread(n - 2);
			tnm1.start();
			tnm2.start();
			try {
				tnm1.join();
				tnm2.join();
			} catch (InterruptedException e) {
				throw new Error();
			}
			this.result = tnm1.getResult() + tnm2.getResult();
		}
	}

	public int getResult() {
		return result;
	}


	public static void main(String[] args) throws InterruptedException {
		FibonacciThread t = new FibonacciThread(19);
		t.start();
		t.join();
		System.out.println(t.getResult());
	}
}
