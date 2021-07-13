import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class SanityCheck {

	public static void main(String[] args) {
		new SanityCheck().sanityCheck(0);
	}

	public void sanityCheck(int h) {
		int l;
		if (h < 4) {
			l = 4 + h;
		} else {
			l = 4;
		}
		Out.print(l);
	}
}