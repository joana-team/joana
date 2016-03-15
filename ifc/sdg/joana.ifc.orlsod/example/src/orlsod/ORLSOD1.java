package orlsod;

public class ORLSOD1 {
	public static boolean HIGH;
	public static int LOW;
	public static int tmp;
	public static int tmp2;
	private static class Thread1 extends Thread {
		public void run() {
			tmp = 1;
			if (HIGH) {
				tmp = 100;
			}
			new Thread2().start();
			while (tmp > 0) {
				tmp = tmp - 1;
			}
			tmp2 = 1;
			new Thread3().start();
			while (tmp2 > 0) {
				tmp2 = tmp2 - 1;
			}
			LOW = 1;
		}
	}
	private static class Thread2 extends Thread {
		public void run() {
			tmp2 = 100;
		}
	}
	private static class Thread3 extends Thread {
		public void run() {
			LOW = 42;
		}
	}

	public static void main(String[] args) {
		new Thread1().start();
	}
}
