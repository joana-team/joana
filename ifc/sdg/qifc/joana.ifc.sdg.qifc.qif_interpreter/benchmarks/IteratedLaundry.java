import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class IteratedLaundry {

	public static void main(String[] args) {
		new IteratedLaundry().launder(0);
	}

	void launder(int h) {
		int out = 0;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < h; ++j) {
				++out;
			}
		}
		Out.print(out);
	}
}