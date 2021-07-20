import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class x_Parity {

	public static void main(String[] args) {
		new x_Parity().parity(0);
	}

	void parity(int h) {
		int parity = 0;
		int bitSet;

		for (int j = 0; j != 32; ++j) {
			bitSet = (h & (1 << j)) != 0 ? 1 : 0;
			parity = (bitSet != parity) ? 1 : 0;
		}
		Out.print(parity);
	}
}