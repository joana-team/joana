package joana.api.testdata.demo;


import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class HighDataConflict {

	static int x;

	public static void main(String[] argv) throws InterruptedException {
		int y = inputPIN();
		new Thread1().start();
		while (y > 0) {
			y--;
		}
		x = 0;
		outputPIN(x);
	}

	static class Thread1 extends Thread {
		public void run() {
			x = 1;
		}
	}

	@Source
	public static int inputPIN() { return 42; }
	@Sink(level="high")
	public static void outputPIN(int pin) {}
}