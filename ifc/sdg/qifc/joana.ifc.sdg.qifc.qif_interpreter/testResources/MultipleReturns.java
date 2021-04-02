import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class MultipleReturns {

	public  static void main(String[] args) {

		MultipleReturns c = new MultipleReturns();
		c.f(0);

	}

	public int f(int h) {
		int l = g(h);
		Out.print(l);
		return 0;
	}

	public int g(int x) {
		if (x < 0) return -1;
		if (x < 2) return 0;
		return 1;
	}
}