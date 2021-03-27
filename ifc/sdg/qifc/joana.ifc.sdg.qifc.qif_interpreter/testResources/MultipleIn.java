import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class MultipleIn {

	public  static void main(String[] args) {

		MultipleIn a = new MultipleIn();
		a.f(1, 0);

	}

	public int f(int h1, int h2) {
		int l = 2;
		h1 = l + h2;
		h2++;
		l = h1 - h2;
		Out.print(h2);
		return 0;
	}
}