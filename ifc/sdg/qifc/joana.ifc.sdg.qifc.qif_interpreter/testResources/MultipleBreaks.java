import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class MultipleBreaks {

	public  static void main(String[] args) {

		MultipleBreaks a = new MultipleBreaks();
		a.f(1);

	}

	public int f(int h) {
		int l = 0;
		while (h >= l) {
			l++;
			if (l == 3) {
				break;
			}
			if (l == 2) {
				break;
			}
			l++;
		}
		Out.print(l);
		return l;
	}

}