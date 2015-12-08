package orlsod;


public class LateSecretAccess {
	public static int HIGH;
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
	public static void main(String[] args) throws InterruptedException {
		Thread t1 = new ThreadA();
		Thread t2 = new ThreadB();
		t1.start();
		t2.start();
		t1.join();
		t2.join();
		int tmp = HIGH;
		while (tmp > 0) {
			tmp--;
		}
	}
}
