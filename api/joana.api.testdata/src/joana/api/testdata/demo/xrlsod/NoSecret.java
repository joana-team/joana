package joana.api.testdata.demo.xrlsod;

import edu.kit.joana.ui.annotations.Sink;

public class NoSecret {
	@Sink
	public static int LOW;
	static class ThreadA extends Thread {
		public void run() {
			LOW = 1;
		}
	}
	static class ThreadB extends Thread {
		public void run() {
			LOW = 2;
		}
	}
	public static void main(String[] args) {
		new ThreadA().start();
		new ThreadB().start();
	}
}
