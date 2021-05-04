import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Comparison {

	public static void main(String[] args) {

		Comparison a = new Comparison();
		a.f(1);

	}

	public int f(int h) {
		int l;
		if (h >= 0 && h < 3) {
			l = 1;
		} else {
			l = 0;
		}
		Out.print(l);
		return 0;
	}
}
