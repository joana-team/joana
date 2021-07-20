import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class x_CallMask {

	public static void main(String[] args) {
		new x_CallMask().mask(0);
	}

	public void mask(int h) {
		h = mask0123(h);
		Out.print(h);
	}

	public int mask0(int h) {
		return h | (1 << 0);
	}

	public int mask1(int h) {
		return h | (1 << 1);
	}

	public int mask2(int h) {
		return h | (1 << 2);
	}

	public int mask3(int h) {
		return h | (1 << 3);
	}

	public int mask01(int h) {
		return mask1(mask0(h));
	}

	public int mask012(int h) {
		return mask2(mask01(h));
	}

	public int mask0123(int h) {
		return mask3(mask012(h));
	}

}