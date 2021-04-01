import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Call {

	public  static void main(String[] args) {

		Call c = new Call();
		c.f(0);

	}

	public int f(int h) {
		int l = -1;
		l = add(l, h);
		Out.print(l);
		return 0;
	}

	public int add(int x, int y) {
		return x + y;
	}
}