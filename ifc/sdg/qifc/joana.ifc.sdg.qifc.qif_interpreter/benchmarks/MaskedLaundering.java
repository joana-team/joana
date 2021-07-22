import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class MaskedLaundering {

	public static void main(String[] args) {
		new MaskedLaundering().laundry(0);
	}

	void laundry(int h) {
		int out = 0;
		while (out != h) {
			out++;
		}
		if ((h & 1) != 0) {
			out = 1;
		}
		Out.print(out);
	}
}