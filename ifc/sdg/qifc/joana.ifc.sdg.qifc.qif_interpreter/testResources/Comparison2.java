import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Comparison2 {

	public static void main(String[] args) {

		Comparison2 a = new Comparison2();
		a.f(1);

	}

	public int f(int h) {
		int l;
		if (h > 3 || h < 0) {
			l = 1;
		} else {
			l = 0;
		}
		Out.print(l);
		return 0;
	}
}
