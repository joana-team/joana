import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class RecursiveLaundry {

	public static void main(String[] args) {
		new RecursiveLaundry().recursiveLaundry(0);
	}

	int launder(int h, int l) {
		if (h == l) {
			return l;
		}

		return launder(h, l + 1);
	}

	void recursiveLaundry(int h) {
		int out = launder(h, 0);
		Out.print(out);
	}

}