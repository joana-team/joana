package joana.api.testdata.demo;


import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class Fig3_2 {

	static int l, h, l1, h1;
	
	public static class O {
		int c;
	}
	
	public static void main(String[] argv) throws InterruptedException {
		h = inputPIN();
		O o1 = new O();
		O o2 = new O();
		o1.c = h;
		o2.c = l;
		O o3 = o2;
		print(o3.c);
		O o4 = o1;
		o4 = o2;
	}

	@Source
	public static int inputPIN() { return 42; }
	@Sink
	public static void print(int s) {}
	public static int input() { return 13; }
	
}