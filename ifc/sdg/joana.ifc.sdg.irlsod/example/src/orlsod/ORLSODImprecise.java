package orlsod;

public class ORLSODImprecise {
	static int HIGH;
	static int H;
	static int H2;
	static int X;
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
