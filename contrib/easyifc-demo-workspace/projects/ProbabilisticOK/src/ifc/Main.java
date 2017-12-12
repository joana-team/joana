package ifc;

import edu.kit.joana.ui.annotations.EntryPoint;
import edu.kit.joana.ui.annotations.EntryPointKind;
import edu.kit.joana.ui.annotations.Level;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class Main {

	static int l, h;
	
	@EntryPoint(kind= EntryPointKind.CONCURRENT)
	public static void main(String[] argv) throws InterruptedException {
		new Thread_1().start();
		new Thread_2().start();
	}

	static class Thread_1 extends Thread {
		
		public void run() {
			Main.l = 0;
			Main.h = Main.inputPIN();
		}
	
	}
	
	static class Thread_2 extends Thread {
		
		public void run() {
			//Main.l = Main.h;
			Main.print(Main.l);
			Main.l = Main.h;
		}
	
	}
	
	@Source(level = Level.HIGH, lineNumber = 38, columnNumber = 1)
	public static int inputPIN() { return 42; }
	
	@Sink(level = Level.LOW, lineNumber = 41, columnNumber = 1)
	public static void print(int i) {}

}