import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class CallTwice {

	public  static void main(String[] args) {

		CallTwice c = new CallTwice();
		c.f(0);

	}

	public int f(int h) {
		int l = 1;
		l = add(l, h);
		l = add(l, l);
		Out.print(l);
		return 0;
	}

	public int add(int x, int y) {
		return x + y;
	}
}