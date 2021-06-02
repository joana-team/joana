import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Loop10 {

	public static void main(String[] args) {

		Loop10 l = new Loop10();
		l.f(0);

	}

	public int f(int h) {
		int l = 0;
		while (h > l) {
			l++;
			h = h & 1;
		}
		Out.print(l);
		return l;
	}
}