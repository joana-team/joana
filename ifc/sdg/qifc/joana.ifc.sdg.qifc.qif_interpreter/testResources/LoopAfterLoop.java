import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class LoopAfterLoop {

	public static void main(String[] args) {

		LoopAfterLoop l = new LoopAfterLoop();
		l.f(0);

	}

	public int f(int h) {
		int l = 0;
		l++;
		while (h > l) {
			l++;
		}
		int i = 0;
		while (i < 16) {
			h--;
			i++;
		}
		Out.print(l);
		return l;
	}
}