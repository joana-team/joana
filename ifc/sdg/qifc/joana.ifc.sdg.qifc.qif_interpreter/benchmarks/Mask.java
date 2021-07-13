import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Mask {

	public static void main(String[] args) {
		new Mask().mask(0);
	}

	public void mask(int h) {
		int l = 4 + (h & 3);
		Out.print(l);
	}
}