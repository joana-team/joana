public class Main {

	static int x, y;
	
	public static void main(String[] argv) throws InterruptedException {
		A a = new A();
		a.start();
		x = input();
		print(x);
//		new A().start();
	}

	static class A extends Thread {
		
		public void run() {
			int y = Main.inputPIN();
			Main.x = y;
		}
	
	}

	//@Source(Level.HIGH)
	public static int inputPIN() { return 42; }
	//@Sink(Level.LOW)
	public static void print(int i) {}
	public static int input() { return 13; }

}