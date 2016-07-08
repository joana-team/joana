package orlsod;

public class WeirdThreads {
	public static class Thread1 extends Thread {
		public void run() {
			spawnThread3Indirect();
		}
	}
	public static class Thread2 extends Thread {
		public void run() {
			spawnThread3Indirect();
		}
	}
	public static class Thread3 extends Thread {
		public void run() {
			@SuppressWarnings("unused")
			int x = 1;
		}
	}
	public static void spawnThread3Indirect() {
		spawnThread3();
	}
	public static void spawnThread3() {
		Thread t3 = new Thread3();
		t3.start();
	}
	public static void main(String[] args) {
		new Thread1().start();
		new Thread2().start();
	}
}
