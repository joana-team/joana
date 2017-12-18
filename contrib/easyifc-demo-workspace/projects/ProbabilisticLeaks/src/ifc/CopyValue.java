package ifc;

import edu.kit.joana.ui.annotations.EntryPoint;
import edu.kit.joana.ui.annotations.EntryPointKind;
import edu.kit.joana.ui.annotations.Level;
import edu.kit.joana.ui.annotations.PointsToPrecision;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

@SuppressWarnings("unused")
public class CopyValue {
	static volatile int x, y;
	final static int secret = 0b1001111111111001000000000000000;
	final static int runs = 1000000;
	
	@EntryPoint(kind = EntryPointKind.CONCURRENT, pointsToPrecision = PointsToPrecision.OBJECT_SENSITIVE)
	public static void main(String[] argv) throws InterruptedException {
		y = inputPIN();
		for (int i = 31; i > 0; i--) {
			int bit = y & (1 << 30);
			A a = new A();
			a.start();
			if (bit != 0) {
				int n = delay(runs);
			}
			x = 1;
			y = y << 1;
			a.join();
			print(x);
			
		}
	}

	static class A extends Thread {
		
		public void run() {
			int n = delay(runs/2);
			x = 0;
		}
	}
	
	public static int delay(int amount) {
		int n = 1;
		for (int k=1; k < 100; k++) {
			for (int j=1; j < amount; j++) {
				n = n * k;
			}
		}
		return n;
	}
	@Source(level = Level.HIGH, lineNumber = 51, columnNumber = 1)
	public static int inputPIN() { return secret; }
	@Sink(level = Level.LOW, lineNumber = 53, columnNumber = 1)
	public static void print(int i) {
		System.out.print(i);
	}

}