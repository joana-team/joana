package joana.api.testdata.demo;


import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class MHPSources {

	public static void main(String[] argv) throws InterruptedException {
		new Thread1().start();
		inputPIN();
	}

	@Source
	public static int inputPIN() { return 42; }

	static class Thread1 extends Thread {
		public void run() {
			inputPIN();
		}
	}
}