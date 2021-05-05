import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Comparison4 {

	public  static void main(String[] args) {

		Comparison4 a = new Comparison4();
		a.f(1);

	}

	public int f(int h) {
		int i = 0;
		while (i < 3) {
			i++;
		}
		if (h > 0) {
			i = h - 1;
		} else {
			i = 0;
		}
		Out.print(i);
		return 0;
	}

}
