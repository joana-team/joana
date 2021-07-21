import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class x_ImplicitFlow {

	public static void main(String[] args) {
		new x_ImplicitFlow().implicit(0);
	}

	public void implicit(int h) {
		int l = 0;
		if (h == 1) {
			l = 1;
		}
		if (h == 1) {
			l = 1;
		}
		if (h == 2) {
			l = 2;
		}
		if (h == 3) {
			l = 3;
		}
		if (h == 4) {
			l = 4;
		}
		if (h == 5) {
			l = 5;
		}
		if (h == 6) {
			l = 6;
		}
		if (h == 7) {
			l = 0;
		}
		Out.print(l);
	}
}