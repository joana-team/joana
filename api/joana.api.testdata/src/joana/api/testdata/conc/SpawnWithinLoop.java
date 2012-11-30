/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.conc;


/**
 * In this example, a thread is spawned within a loop. Analysis should determine one dynamic thread instance.
 * @author Martin Mohr
 *
 */
public class SpawnWithinLoop {
	
	
	public static void main(String[] args) {
		for (int i = 0; i < 3; i++) {
			SimpleThread s = new SimpleThread();
			s.start();
		}
	}
}
