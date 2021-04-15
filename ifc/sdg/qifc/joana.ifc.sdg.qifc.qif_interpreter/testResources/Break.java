import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Break {

	public  static void main(String[] args) {

		Break a = new Break();
		a.f(1);

	}

	public int f(int h) {
		int l = 1;
		while (0 <= l) {
			l = l & h;
			if (l == 2) {
				break;
			}
			l = -1;
		}
		Out.print(l);
		return l;
	}

}