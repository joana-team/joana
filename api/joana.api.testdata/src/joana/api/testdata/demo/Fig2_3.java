package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class Fig2_3 {

	static int l, h;
	
	public static void main(String[] argv) throws InterruptedException {
		new Thread_1().start();
		new Thread_2().start();
	}

	static class Thread_1 extends Thread {
		public void run() {
			longCmd();
			print("PO");
		}
	}

	static class Thread_2 extends Thread {
		public void run() {
			h = inputPIN();
			while (h != 0)
				h--;
			print("ST");
		}
	}
	
	@Source
	public static int inputPIN() { return 42; }
	@Sink
	public static void print(String s) {}
	public static int input() { return 13; }

	private static void longCmd() {
		for (int i = 0; i < 1000; i++) {
			l *= 12;
		}
	}
	
}