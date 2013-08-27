package joana.api.testdata.conc;
/**
 * @author Martin Mohr
 */
public class GiffhornFigure51 {
	
	static class Thread1 extends Thread {
		public void run() {
			x = 0;
			System.out.println(x); // low sink
		}
	}
	
	static class Thread2 extends Thread {
		public void run() {
			int y = inputPIN(); // high source
			while (y != 0) {
				y--;
			}
			x = 1;
			System.out.println(2); // low sink
		}
		
		private int inputPIN() {
			return 42;
		}
	}
	
	static int x;
	
	public static void main(String[] args) throws InterruptedException {
		Thread1 t1 = new Thread1();
		Thread2 t2 = new Thread2();
		
		t1.start();
		t2.start();
	}
	
	
	
}
