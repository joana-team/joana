import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class IrrelevantIf {

	public  static void main(String[] args) {

		IrrelevantIf if_ = new IrrelevantIf();
		if_.f(0);

	}

	public int f(int h) {
		int l = 0;
		if (h > 0) {
			l = 1 + l;
		}
		l = 0;
		Out.print(l);
		return l;
	}
}