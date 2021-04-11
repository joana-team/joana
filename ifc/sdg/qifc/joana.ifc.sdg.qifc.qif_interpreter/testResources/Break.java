import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Break {

	public  static void main(String[] args) {

		Break a = new Break();
		a.f(1);

	}

	public int f(int h) {
		int l = 0;
		while (h >= l) {
			l++;
			if (l == 3) {
				break;
			}
			l++;
		}
		Out.print(l);
		return l;
	}

}