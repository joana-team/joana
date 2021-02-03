import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class SimpleArithmetic {

	public  static void main(String[] args) {

		SimpleArithmetic a = new SimpleArithmetic();
		a.f(1, 0);

	}

	public int f(int h1, int h2) {
		int l = 2;
		h1 = l + h2;
		h2++;
		l = h1 - h2;
		Out.print(h1); // 2
		Out.print(h2); // 1
		Out.print(l); // 1
		return 0;
	}
}