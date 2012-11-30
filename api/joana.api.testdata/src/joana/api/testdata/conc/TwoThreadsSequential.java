/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.conc;

/**
 * In this test, two threads are spawned. The second thread is spawned after the first thread has terminated.
 * @author Martin Mohr
 */
public class TwoThreadsSequential {
	
	public static void main(String[] args) throws InterruptedException {
		Thread t1 = new SimpleThread();
		Thread t2 = new SimpleThread();
		t1.start();
		t1.join();
		t2.start();
		
	}
}
