import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class MaskedCopy {

	public static void main(String[] args) {
		new MaskedCopy().mask(0);
	}

	public void mask(int h) {
		int l = h & (-1 << 16);
		Out.print(l);
	}
}