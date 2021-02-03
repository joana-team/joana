import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class If {

	public  static void main(String[] args) {

		If if_ = new If();
		if_.f(0);

	}

	public int f(int h) {
		Out.print(h);
		int l = 0;
		if (h > 0) {
			l = 1;
		}
		Out.print(l);
		return l;
	}

}