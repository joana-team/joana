import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Break2 {

	public  static void main(String[] args) {

		Break2 a = new Break2();
		a.f(1);

	}

	public int f(int h) {
		int l = 0;
		while (l >= 0) {
			if (h == 0) {
				l++;
			} else {
				l--;
			}
			if (l == 1) {
				break;
			}
		}
		Out.print(l);
		return l;
	}

}