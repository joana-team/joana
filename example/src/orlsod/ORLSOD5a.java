package orlsod;


public class ORLSOD5a {
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
		int tmp = HIGH;
		while (tmp > 0) {
			tmp--;
		}
		for (int i = 0; i < tmp; i++) {
			Thread t1 = new ThreadA();
			Thread t2 = new ThreadB();
			t1.start();
			t2.start();
		}
	}
}
