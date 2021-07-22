import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class ShiftAndLaunder {

	public static void main(String[] args) {
		new ShiftAndLaunder().sal(0);
	}

	void sal(int h1) {
		int launder = 0;
		int shift = 1;
		int i = 0;

		while (i != h1) {
			launder += 1;
			shift = shift << 1;
			i++;
		}

		Out.print(launder);
	}

}