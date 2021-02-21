import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class IfinLoop {

	public  static void main(String[] args) {

		IfinLoop if_ = new IfinLoop();
		if_.f(0);

	}

	public int f(int h) {
		int l = 0;
		while (h > 0) {
			if (l > 0) {
				l = l - 1;
			} else {
				l = 3;
			}
			h--;
		}
		Out.print(l);
		return l;
	}

}