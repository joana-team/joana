import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class LoopInIf {

	public  static void main(String[] args) {

		LoopInIf if_ = new LoopInIf();
		if_.f(0, 1);

	}

	public int f(int h1, int h2) {
		int l = 0;
		if (h1 > 0) {
			while (h2 > 0) {
				l++;
				h2--;
			}
			h1 = 1;
		}
		Out.print(l);
		return l;
	}
}