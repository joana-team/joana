import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Loop7 {

	public static void main(String[] args) {

		Loop7 l = new Loop7();
		l.f(0);

	}

	public int f(int h) {
		int l = 0;
		while (3 > l) {
			l++;
			if (l == h) {
				break;
			}
			l++;
		}
		Out.print(l);
		return l;
	}
}
