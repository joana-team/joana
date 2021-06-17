import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class ImplicitFlow {

	public  static void main(String[] args) {

		ImplicitFlow if_ = new ImplicitFlow();
		if_.f(0);

	}

	public int f(int h) {
		int l = 0;
		if (h == 42) {
			l = 1;
		}
		Out.print(l);
		return l;
	}

}