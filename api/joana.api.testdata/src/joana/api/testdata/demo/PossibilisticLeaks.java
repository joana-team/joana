package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

/*
 * This program is not LSOD, because "print(x)", which is low-observable, can print 13 or 0
 * depending on the execution order of "x = input()" and "x = y".
 * It is RLSOD (and iRLSOD), however, since that conflict is not influenced by "inputPIN()".
 */
public class PossibilisticLeaks {

	static int x, y;
	
	public static void main(String[] argv) throws InterruptedException {
		Thread_2 t = new Thread_2();
		t.start();
		x = input();
		print(x);
	}

	static class Thread_2 extends Thread {
		public void run() {
			x = y;
			y = inputPIN();
		}
	}

	@Source
	public static int inputPIN() { return 42; }
	@Sink
	public static void print(int i) {}
	public static int input() { return 13; }

}