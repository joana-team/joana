import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class If2 {

	public  static void main(String[] args) {

		If2 if_ = new If2();
		if_.f(0);

	}

	public int f(int h) {
		int l = 0;
		if (h > 0) {
			l = 1;
		} else {
			l = 0;
		}
		Out.print(l);
		return l;
	}
}