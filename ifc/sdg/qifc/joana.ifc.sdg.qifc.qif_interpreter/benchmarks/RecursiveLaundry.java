import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class RecursiveLaundry {

	public static void main(String[] args) {
		new RecursiveLaundry().recursiveLaundry(0);
	}

	int launder(int h) {
		if (h <= 0) {
			return 0;
		}

		return launder(h - 1) + 1;
	}

	void recursiveLaundry(int h) {
		int out = launder(h);
		Out.print(out);
	}

}