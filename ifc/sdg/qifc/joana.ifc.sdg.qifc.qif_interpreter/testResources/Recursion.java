import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Recursion {

	public  static void main(String[] args) {

		Recursion c = new Recursion();
		c.f(0);

	}

	public int f(int n) {
		Out.print(rec(n));
		return 0;
	}

	public int rec(int n) {
		if ((n & 1) == 0) {
			return 0;
		} else {
			return rec(n & -2);
		}
	}
}