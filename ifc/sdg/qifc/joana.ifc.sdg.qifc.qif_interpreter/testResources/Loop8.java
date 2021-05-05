import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Loop8 {

	public static void main(String[] args) {

		Loop8 l = new Loop8();
		l.f(0);

	}

	public int f(int h) {
		int l = 0;
		while (3 > l) {
			l++;
			if (l == h || l == 2) {
				break;
			}
			l++;
		}
		Out.print(l);
		return l;
	}
}
