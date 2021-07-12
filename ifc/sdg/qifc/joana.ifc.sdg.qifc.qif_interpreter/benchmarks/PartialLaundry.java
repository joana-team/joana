import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class PartialLaundry {

	public static void main(String[] args) {
		new Laundry().laundry(0);
	}

	void partialLaundry(int h) {
		int out = 0;

		for (int i = 0; i < h; ++i) {
			if (i < 15) {
				++out;
			}
		}

		Out.print(out);
	}

}