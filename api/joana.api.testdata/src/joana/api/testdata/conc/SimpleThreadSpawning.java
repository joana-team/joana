package joana.api.testdata.conc;
/**
 * In this example, one single thread is spawned. The thread invocation analysis should conclude, that
 * there is only one instance of this thread.
 * @author Martin Mohr
 */
public class SimpleThreadSpawning {
	
	
	public static void main(String[] args) {
		Thread t = new SimpleThread();
		t.start();
	}
}
