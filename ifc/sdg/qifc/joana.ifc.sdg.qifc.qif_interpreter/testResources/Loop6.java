import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Loop6 {

	public static void main(String[] args) {

		Loop6 l = new Loop6();
		l.f(0);

	}

	public int f(int h) {
		int l = 0;
		while (3 > l) {
			l++;
			if (l == h) {
				break;
			}
		}
		Out.print(l);
		return l;
	}
}
