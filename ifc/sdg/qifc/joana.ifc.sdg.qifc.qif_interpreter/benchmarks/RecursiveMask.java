import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class RecursiveMask {

	public static void main(String[] args) {
		new RecursiveMask().mask(0);
	}

	public void mask(int h) {
		h = recMask(h, 0);
		Out.print(h);
	}

	public int recMask(int h, int i) {
		if (i == 32) {
			return h;
		}
		return recMask(h | (1 << i), i + 1);
	}
}