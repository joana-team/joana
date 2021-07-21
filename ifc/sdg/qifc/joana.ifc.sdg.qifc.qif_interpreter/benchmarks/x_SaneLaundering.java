import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class x_SaneLaundering {

	public static void main(String[] args) {
		new x_SaneLaundering().sanityCheck(0);
	}

	public void sanityCheck(int h) {
		int l;
		if (0 <= h && h < 16) {
			l = 3 + h;
		} else {
			l = 3;
		}

		int out = 0;
		for (int i = 0; i != l; ++i) {
			++out;
		}

		Out.print(out);
	}
}