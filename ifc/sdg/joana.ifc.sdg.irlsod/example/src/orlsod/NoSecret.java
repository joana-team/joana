package orlsod;

public class NoSecret {
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
