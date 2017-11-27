package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

class Z {
	int z;
}

public class NullPointerExceptionImpossibleNoFlow {

	static int x, y;
	
	public static void main(String[] argv) throws InterruptedException {
		print(x);
		Z z = new Z();
		Thread_2 t = new Thread_2();
		t.start();
		x = input();
		try {
			int access = z.z;
		} catch (NullPointerException e) {
			print(x);
		}
	}

	static class Thread_2 extends Thread {
		public void run() {
			y = inputPIN();
			x = y;
		}
	}

	@Source
	public static int inputPIN() { return 42; }
	@Sink
	public static void print(int i) {}
	public static int input() { return 13; }

}