import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Loop5 {

	public static void main(String[] args) {

		Loop5 l = new Loop5();
		l.f(0);

	}

	public int f(int h) {
		int l = 0;
		while (3 > l) {
			if (l == h) {
				break;
			}
			l++;
		}
		Out.print(l);
		return l;
	}
}
