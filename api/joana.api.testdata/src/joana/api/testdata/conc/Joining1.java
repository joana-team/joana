/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.conc;


/**
 * This short example illustrates that in the presence of joins, thread interference cannot be
 * excluded just because the interfering statements may not happen in parallel.
 * The main thread spawns a thread which writes to a shared variable. This write has influence on
 * the System.out.println() statement in the main thread because the println statement is guaranteed
 * to be executed after the write statement, due to joining. 
 * This means, that the interference edge which is initially assumed between the two statements cannot
 * be removed, regardless of the fact that the {@link PreciseMHPAnalysis precise MHP analysis} concludes
 * that they cannot happen in parallel.
 * @author Martin Mohr &lt; martin.mohr@kit.edu &gt;
 */
public class Joining1 {
	
	public static int f;
	
	public static void main(String[] args) throws InterruptedException {
		Thread0 t = new Thread0();
		f = 2;
		t.start();
		t.join();
		System.out.println(f); // low
	}
	
}


class Thread0 extends Thread {
	public void run() {
		Joining1.f = 3; // high
	}
}
