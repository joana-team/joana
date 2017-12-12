package ifc;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;
import static edu.kit.joana.ui.annotations.Level.*;

import edu.kit.joana.ui.annotations.EntryPoint;
import edu.kit.joana.ui.annotations.EntryPointKind;

public class Main {

	static int x, y;
	
	@EntryPoint(kind = EntryPointKind.CONCURRENT)
	public static void main(String[] argv) throws InterruptedException {
		A a = new A();
		a.start();
		x = input();
		print(x);
		new A().start();
	}

	static class A extends Thread {
		
		public void run() {
			int y = Main.inputPIN();
			Main.x = y;
		}
	
	}
	
	@Source(level = HIGH, lineNumber = 32, columnNumber = 1)
	public static int inputPIN() { return 42; }
	
	@Sink(level = LOW, lineNumber = 35, columnNumber = 1)
	public static void print(int i) {}
	
	@Source(level = LOW, lineNumber = 38, columnNumber = 1)
	public static int input() { return 13; }

}