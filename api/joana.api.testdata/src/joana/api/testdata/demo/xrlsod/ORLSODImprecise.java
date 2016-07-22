package joana.api.testdata.demo.xrlsod;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class ORLSODImprecise {
	@Source
	static int HIGH;
	static int H;
	static int H2;
	static int X;
	@Sink
	static int LOW;
	static class Thread2 extends Thread {
		public void run() {
			H2 = H;
			X = 42;
			LOW = X;
		}
	}
	public static void main(String[] args) {
		H = 0;
		Thread t = new Thread2();
		t.start();
		H = HIGH;
		H = H + 1;
	}
}
