package orlsod;

public class ORLSOD5b {
	public static int HIGH;
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
