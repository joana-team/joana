/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.conc;

/**
 * In this example, a thread is spawned after a loop is finished. Analysis should assume conservatively
 * that this thread is spawned but identify it as non-dynamic.
 * @author Martin Mohr
 */
public class SpawnAfterLoop {
	
	public static void main(String[] args) {
		Thread t = new SimpleThread();
		
		while (args[0].contains("hallo welt")) {
			;
		}
		
		t.start();
	}
}
