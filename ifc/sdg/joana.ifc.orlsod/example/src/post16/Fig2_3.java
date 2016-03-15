package post16;


public class Fig2_3 {

	public static int l, h;
	public static String LOW;
	public static int HIGH;

	public static void main(String[] argv) throws InterruptedException {
		Thread_1 t1 = new Thread_1(); t1.start();
//		t1.join();
		new Thread_2().start();
//		h = inputPIN();
//		while (h != 0)
//			h--;
//		print("ST");
	}

	static class Thread_1 extends Thread {
		public void run() {
			longCmd();
			LOW = "PO";
		}
	}

	static class Thread_2 extends Thread {
		public void run() {
			h = HIGH;
			while (h != 0)
				h--;
			LOW = "ST";
		}
	}

	public static int inputPIN() { return 42; }
	public static void print(String s) {}
	public static int input() { return 13; }

	private static void longCmd() {
		for (int i = 0; i < 1000; i++) {
			l *= 12;
		}
	}

}