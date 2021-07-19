import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class x_Mask {

	public static void main(String[] args) {
		new x_Mask().mask(0);
	}

	public void mask(int h) {
		int l = h & (-1 << 16);
		Out.print(l);
	}
}