package orlsod;

public class RecursiveThreads {
	public static class Thread1 extends Thread {
		public void run() {
			new Thread2().start();
		}
	}
	public static class Thread2 extends Thread {
		public void run() {
			new Thread1().start();
		}
	}
	public static void main(String[] args) {
		new Thread1().start();
	}
}
