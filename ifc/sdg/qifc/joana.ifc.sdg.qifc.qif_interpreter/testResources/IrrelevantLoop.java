import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class IrrelevantLoop {

	public static void main(String[] args) {

		IrrelevantLoop l = new IrrelevantLoop();
		l.f(0);

	}

	public int f(int h) {
		int l = 0;
		int x = h;

		while (h > l) {
			l++;
		}

		Out.print(x + 1);
		return l;
	}
}