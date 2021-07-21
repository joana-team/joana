import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Mask2 {

	public static void main(String[] args) {
		new Mask2().laundry(0);
	}

	void laundry(int h) {
		int out = h;
		if ((h & 1) != 0) {
			out = 1;
		}
		Out.print(out);
	}
}