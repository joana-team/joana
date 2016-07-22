package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

/*
 * This program is LSOD, however, JOANA will find 2 alarms:
 * 1. It finds a data conflict between "l = 0;" and "print(l);".
 *    This would be a true conflict if the value assigned to "l"
 *    would not be "0" which is also the value "l" would have if "print(l)"
 *    is executed first. Since JOANA does not check the explicit value,
 *    it will generate a violation.
 * 2. It finds a (true) data-write conflict between "l = 0;" and "l = h;". It also
 *    thinks, however, that this conflict influences "print(l);". This is not possible
 *    because "l = h;" will always happen after the print statement.
 */
public class ProbabilisticOK {

	static int l, h;
	
	public static void main(String[] argv) throws InterruptedException {
		new Thread_1().start();
		new Thread_2().start();
	}

	static class Thread_1 extends Thread {
		public void run() {
			l = 0;
			h = inputPIN();
		}
	}
	
	static class Thread_2 extends Thread {
		public void run() {
			print(l);
			l = h;
		}
	}
	
	@Source
	public static int inputPIN() { return 42; }
	@Sink
	public static void print(int i) {}

}