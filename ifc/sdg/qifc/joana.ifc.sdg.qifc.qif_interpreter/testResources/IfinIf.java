import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class IfinIf {

	public  static void main(String[] args) {

		IfinIf if_ = new IfinIf();
		if_.f(0);

	}

	public int f(int h) {
		int l = 0;
		if (h > 0) {
			if (h <= 1) {
				l = 1 + l;
			} else {
				l = 2 + l;
			}
		}
		Out.print(l);
		return l;
	}
}