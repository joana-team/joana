public class Main {

	static int x, y;
	
	public static void main(String[] argv) throws InterruptedException {
		A a = new A();
		a.start();
//		a.join();
		y = inputPIN();
		while (y != 0)
			y--;
		x = 1;
		print(2);
//		new A().start();
	}

	static class A extends Thread {
		
		public void run() {
			Main.x = 0;
			Main.print(Main.x);
		}
	
	}
	
	//@Source(Level.HIGH)
	public static int inputPIN() { return 42; }
	//@Sink(Level.LOW)
	public static void print(int i) {}
	public static int input() { return 13; }

}