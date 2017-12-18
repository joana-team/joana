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
		for (int i = 31; i > 0;) {
			if (y != 0) {
				while (true) {
				}
			}
			print(1);
			
		}
	}

	@Source(level = Level.HIGH, lineNumber = 29, columnNumber = 1)
	public static int inputPIN() { return secret; }

	@Sink(level = Level.LOW, lineNumber = 32, columnNumber = 1)
	public static void print(int i) {
		System.out.print(i);
	}

}
