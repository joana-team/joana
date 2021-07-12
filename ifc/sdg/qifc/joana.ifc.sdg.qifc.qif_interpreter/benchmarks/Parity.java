import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Parity {

	public static void main(String[] args) {
		new Parity().parity(0);
	}

	void parity(int h) {
		int parity = 0;
		int bitSet;

		int k = 1;
		for (int j = 0; j < 8; ++j) {

			bitSet = (h & k) != 0 ? 1 : 0;
			parity = (bitSet != parity) ? 1 : 0;
			k *= 2;
		}

		Out.print(parity);
	}

}