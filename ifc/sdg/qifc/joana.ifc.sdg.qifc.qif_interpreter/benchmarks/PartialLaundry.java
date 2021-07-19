import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class PartialLaundry {

	public static void main(String[] args) {
		new PartialLaundry().partialLaundry(0);
	}

	void partialLaundry(int h) {
		int out = 0;
		int i = 0;
		while (i != h) {
			if (i < 1) {
				++out;
			}
			i++;
		}

		Out.print(out);
	}

}