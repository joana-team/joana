import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class IrrelevantIf2 {

	public static void main(String[] args) {

		IrrelevantIf2 if_ = new IrrelevantIf2();
		if_.f(0);

	}

	public int f(int h) {
		int l;
		if ((h | -1) == -1) {
			l = 2;
		} else {
			l = h;
		}
		Out.print(l);
		return l;
	}
}