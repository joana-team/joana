import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Call2 {

	public static void main(String[] args) {

		Call2 c = new Call2();
		c.f(0);

	}

	public int f(int h) {
		int l = -1;
		h |= 1;
		int k = -1;
		l = add(l, h);
		l |= k;
		Out.print(l);
		return 0;
	}

	public int add(int x, int y) {
		return x + y;
	}
}