import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Laundry {

	public static void main(String[] args) {
		new Laundry().laundry(0);
	}

	void laundry(int h) {
		int out = 0;
		for (int i = 0; i < h; ++i) {
			++out;
		}
		Out.print(out);
	}

}