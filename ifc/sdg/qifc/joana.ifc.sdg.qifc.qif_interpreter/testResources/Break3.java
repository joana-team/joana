import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Break3 {

	public static void main(String[] args) {

		Break3 a = new Break3();
		a.f(1);

	}

	public int f(int h) {
		int l = 1;
		while (0 <= l) {
			l = l & h;
			if (l == 2) {
				l = h + 2;
				if (h == 0) {
					break;
				}
				break;
			}
			l = -1;
		}
		Out.print(l);
		return l;
	}

}