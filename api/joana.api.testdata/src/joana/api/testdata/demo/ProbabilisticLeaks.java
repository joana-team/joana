package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class ProbabilisticLeaks {

	static int x, y;
	
	public static void main(String[] argv) throws InterruptedException {
		Thread_1 t = new Thread_1();
		t.start();
		y = inputPIN();
		while (y != 0)
			y--;
		print("P");
	}

	static class Thread_1 extends Thread {
		public void run() {
			print("SA");
		}
	}
	
	@Source
	public static int inputPIN() { return 42; }
	@Sink
	public static void print(String s) {}
	public static int input() { return 13; }

}