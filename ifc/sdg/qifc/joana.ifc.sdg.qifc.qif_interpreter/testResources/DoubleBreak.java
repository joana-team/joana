import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class DoubleBreak {

	public  static void main(String[] args) {

		DoubleBreak a = new DoubleBreak();
		a.f(1);

	}

	public int f(int h) {
		int l = 0;
		while (h >= l) {
			l++;
			if (l == 0) {
				break;
			}

			if (h == 0) {
				break;
			}
			l++;
		}
		Out.print(l);
		return l;
	}
}