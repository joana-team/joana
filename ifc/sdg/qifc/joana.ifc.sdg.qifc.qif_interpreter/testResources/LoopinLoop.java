import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class LoopinLoop {

	public  static void main(String[] args) {

		LoopinLoop if_ = new LoopinLoop();
		if_.f(0, 1);

	}

	public int f(int h1, int h2) {
		int l = 0;
		while (h1 > 0) {
			while (h2 > 0) {
				l++;
				h2--;
			}
			h1--;
			l++;
		}
		Out.print(l);
		return l;
	}

}