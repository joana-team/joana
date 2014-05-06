public class Main {

	static int x, y;
	
	public static void main(String[] argv) {
		x = inputPIN();
		if (x < 1234)
			print(0);
		y = x;
		print(y);
	}
	
	//@Source(Level.HIGH)
	public static int inputPIN() { return 42; }
	//@Sink(Level.LOW)
	public static void print(int i) {}

}
