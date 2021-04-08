import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Recursion2 {

	public  static void main(String[] args) {

		Recursion2 c = new Recursion2();
		c.f(0);

	}

	public int f(int n) {
		Out.print(g(n));
		return 0;
	}

	public int g(int n) {
		if (n <= 0) {
			return 0;
		}
		return 1 + g(0);
	}
}