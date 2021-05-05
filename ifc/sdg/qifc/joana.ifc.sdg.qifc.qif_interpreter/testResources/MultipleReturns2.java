import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class MultipleReturns2 {

	public  static void main(String[] args) {

		MultipleReturns2 c = new MultipleReturns2();
		c.f(0);

	}

	public int f(int h) {
		int l = g(h);
		Out.print(l);
		return 0;
	}

	public int g(int x) {
		if (x == -3 || x == -1 || x == -2 || x == -4) return -1;
		if (x <= 2 && x != 2) return 0;
		return 1;
	}
}
