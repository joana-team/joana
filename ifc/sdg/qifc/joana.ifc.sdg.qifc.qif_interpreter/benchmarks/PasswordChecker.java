import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class PasswordChecker {

	public static void main(String[] args) {
		new PasswordChecker().check(0);
	}

	public void check(int h) {
		int l = 0;
		if (h == 0) {
			l = 1;
		}
		Out.print(l);
	}
}