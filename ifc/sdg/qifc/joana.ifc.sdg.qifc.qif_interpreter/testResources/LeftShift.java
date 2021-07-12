import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class LeftShift {

	public  static void main(String[] args) {

		LeftShift or = new LeftShift();
		or.f(1, 0);

	}

	public int f(int h1, int h2) {
		int l = h1 << 2;
		Out.print(l);
		return l;
	}
}