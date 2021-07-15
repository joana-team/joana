import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class SequentialParity {

	public static void main(String[] args) {
		new SequentialParity().parity(0);
	}

	public void parity(int h) {
		int parity = 1;
		int bitSet;

		bitSet = ((h & (1 << 0)) == 0) ? 0 : 1;
		parity = (parity == bitSet) ? 0 : 1;

		bitSet = ((h & (1 << 1)) == 0) ? 0 : 1;
		parity = (parity == bitSet) ? 0 : 1;

		bitSet = ((h & (1 << 2)) == 0) ? 0 : 1;
		parity = (parity == bitSet) ? 0 : 1;

		bitSet = ((h & (1 << 3)) == 0) ? 0 : 1;
		parity = (parity == bitSet) ? 0 : 1;

		bitSet = ((h & (1 << 4)) == 0) ? 0 : 1;
		parity = (parity == bitSet) ? 0 : 1;

		bitSet = ((h & (1 << 5)) == 0) ? 0 : 1;
		parity = (parity == bitSet) ? 0 : 1;

		bitSet = ((h & (1 << 6)) == 0) ? 0 : 1;
		parity = (parity == bitSet) ? 0 : 1;

		bitSet = ((h & (1 << 7)) == 0) ? 0 : 1;
		parity = (parity == bitSet) ? 0 : 1;

		Out.print(parity);
	}
}