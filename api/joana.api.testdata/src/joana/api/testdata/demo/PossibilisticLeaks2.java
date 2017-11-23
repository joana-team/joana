package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

import static edu.kit.joana.ui.annotations.Level.*;

public class PossibilisticLeaks2 {

	static int x, y;
	
	public static void main(String[] argv) throws InterruptedException {
		A a = new A();
		a.start();
		x = input();
		print(x);
		new A().start();
	}

	static class A extends Thread {
		
		public void run() {
			int y = PossibilisticLeaks2.inputPIN();
			PossibilisticLeaks2.x = y;
		}
	
	}
	
	@Source(level = HIGH)
	public static int inputPIN() { return 42; }
	
	@Sink(level = LOW)
	public static void print(int i) {}
	
	@Source(level = LOW)
	public static int input() { return 13; }

}