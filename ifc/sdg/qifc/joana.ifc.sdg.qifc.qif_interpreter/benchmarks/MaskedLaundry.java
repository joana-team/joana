import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class MaskedLaundry {

	public static void main(String[] args) {
		new MaskedLaundry().laundry(0);
	}

	void laundry(int h) {
		int out = 0;

		if ((h & 1) == 0) {
			h = h | 1;
		}
		for (int i = 0; i < h; ++i) {
			++out;
		}
		Out.print(out);
	}
}