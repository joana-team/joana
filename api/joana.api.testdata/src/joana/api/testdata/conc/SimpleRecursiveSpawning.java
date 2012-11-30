/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.conc;

/**
 * In this example, a thread is spawned inside a recursive method. Analysis should identify one dynamic thread and
 * three non-dynamic threads. The dynamic thread is spawned in a context main->recSpawn->...->recSpawn (indefinitely
 * often), and the three non-dynamic threads are:<p/>
 * <ul>
 * <li>the main thread</li>
 * <li>the thread spawned in the context main -> recSpawn</li>
 * <li>the thread spawned in the context main -> recSpawn -> recSpawn</li>
 * </ul>
 * Note, that due to context-sensitivity, there are two instances of the method recSpawn(): One for the call of recSpawn
 * from the main method and one for calls of recSpawn from within itself or the other instance. Consequently, the analysis
 * can distiguish between spawns in the contexts main -> recSpawn and main -> recSpawn -> recSpawn and considers both
 * threads as non-dynamic, since the second arrow in the context "main -> recSpawn -> recSpawn" is not a recursive call
 * but a call between one instance of recSpawn and the other. If n<=0, none of the two spawns can happen. If n=1, then 
 * only the spawn "main -> recSpawn" happens and if n>=2, both spawns and happen. The dynamic thread comprises all threads
 * spawned in a recursive context. 
 * As the analysis abstracts away from values, it must consider all possible values of n and therefore does not discard
 * any possible context.<p/>
 * 
 * @author Martin Mohr
 */
public class SimpleRecursiveSpawning {
	
	public void recSpawn(int n) {
		if (n <= 0) {
			return;
		} else {
			SimpleThread t = new SimpleThread();
			t.start();
			recSpawn(n - 1);
		}
	}


	public static void main(String[] args) {
		new SimpleRecursiveSpawning().recSpawn(42);
	}
	
}
