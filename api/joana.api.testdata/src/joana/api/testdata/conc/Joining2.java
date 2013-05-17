/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.conc;


/**
 * This is a variant of {@link Joining1} where the interfering threads are both spawned from
 * the main thread, instead of the main thread itself being one of them.
 * @author Martin Mohr &lt; martin.mohr@kit.edu &gt;
 */
public class Joining2 {
	
	public static int f;
	public static ThreadA t1 = new ThreadA();
	public static ThreadB t2 = new ThreadB();
	
	public static void main(String[] args) throws InterruptedException {
		t1.start();
		t1.join();
		t2.start();
		t2.join();
	}
	
}


class ThreadA extends Thread {
	public void run() {
		Joining2.f = 3;
	}
}

class ThreadB extends Thread {
	public void run() {
		System.out.println(Joining2.f);	
	}
}
