import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Loop4 {

	public static void main(String[] args) {

		Loop4 l = new Loop4();
		l.f(0);

	}

	public int f(int h) {
		int l = 0;
		while (3 > l && l != h) {
			l++;
		}
		Out.print(l);
		return l;
	}
}
