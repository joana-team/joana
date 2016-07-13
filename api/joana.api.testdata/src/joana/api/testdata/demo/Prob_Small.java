package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Source;

public class Prob_Small {

	static int h;
	
	public static void main(String[] argv) throws InterruptedException {
		new Thread_1().start();
		h = inputPIN();
	}

	static class Thread_1 extends Thread {
		public void run() {
			h = inputPIN();
		}
	}
	
	@Source
	public static int inputPIN() { return 42; }
}