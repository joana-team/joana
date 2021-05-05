import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Recursion3 {

	public  static void main(String[] args) {

		Recursion3 c = new Recursion3();
		c.f(0);

	}

	public int f(int n) {
		Out.print(rec(n));
		return 0;
	}

	public int rec(int n) {
		int m;
		if (n == 0) { m = 1; }
		else if ((n & 1) == 0) {
			m = 0;
		} else {
			m = rec(n & -2);
		}
		return m;
	}

}
