/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.conc;

/**
 * In this example, there is one thread spawned by the main thread. This thread itself spawns yet another instance of its class,
 * leading to indefinitely long chains of thread spawns. The analysis should distinguish between the main thread, the thread t spawned
 * by the main thread and any thread which is spawned by t or a descendant of t. The descendants of t should all be subsumed into
 * one dynamic thread, so the analysis should identify one dynamic thread and two non-dynamic threads (main and t).
 * @author Martin Mohr
 */
public class RecursiveSpawning {
	public static void main(String[] args) {
		new RecursiveThread().start();
	}
}

class RecursiveThread extends Thread {
	
	@Override
	public void run() {
		new RecursiveThread().start();
	}
	
}
