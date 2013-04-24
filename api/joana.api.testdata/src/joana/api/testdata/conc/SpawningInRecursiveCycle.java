/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.conc;

/**
 * In this example, there are two methods which recursively call each other and each spawn a thread afterwards. 
 * Analysis should identify five threads: two of these should be classified as dynamic and three should be classified
 * as non-dynamic. The non-dynamic threads are:
 * <ul>
 * <li>the main thread</li>
 * <li>the thread spawned in the context main -> f</li>
 * <li>the thread spawned in the context main -> f -> g</li>
 * </ul>
 * The dynamic threads are:
 * <ul>
 * <li>the thread spawned in the context main -> f -> g -> f -> ... -> g -> f </li>
 * <li>the thread spawned in the context main -> f -> g -> f -> g ... -> f -> g </li>
 * </ul>
 * 
 * @author Martin Mohr
 */
public class SpawningInRecursiveCycle {
	
	public void f() {
		new SimpleThread().start();
		g();
	}
	
	public void g() {
		new SimpleThread().start();
		f();
	}


	public static void main(String[] args) {
		new SpawningInRecursiveCycle().f();
	}
	
}
