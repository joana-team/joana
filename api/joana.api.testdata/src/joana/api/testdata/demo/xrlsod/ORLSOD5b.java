package joana.api.testdata.demo.xrlsod;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class ORLSOD5b {
	@Source
	public static int HIGH;
	@Sink
	public static int LOW;

	public static class ThreadA extends Thread {
		public void run() {
			LOW = 1;
		}
	}

	public static class ThreadB extends Thread {
		public void run() {
			LOW = 2;
		}
	}


	public static void main(String[] args) {
		int tmp = 0;
		while (tmp < HIGH) {
			tmp++;
		}
		for (int i = 0; i < tmp; i++) {
			Thread t1 = new ThreadA();
			Thread t2 = new ThreadB();
			t1.start();
			t2.start();
		}
	}
}
