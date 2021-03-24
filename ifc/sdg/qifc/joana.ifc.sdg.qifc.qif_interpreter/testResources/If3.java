import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class If3 {

	public  static void main(String[] args) {

		If3 if_ = new If3();
		if_.f(0);

	}

	public int f(int h) {
		int l = 0;
		if (h != 0) {
			l = ~l;
		}
		Out.print(l);
		return l;
	}
}