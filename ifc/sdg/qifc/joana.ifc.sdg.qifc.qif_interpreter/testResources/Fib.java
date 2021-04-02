import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Fib {

	public  static void main(String[] args) {

		Fib c = new Fib();
		c.fib(0);

	}

	public int fib(int n) {
		Out.print(fibRec(n));
		return 0;
	}

	public int fibRec(int n) {
		if (n == 0) {
			return 0;
		}

		if (n == 1) {
			return 1;
		}

		return n + fibRec(n - 1);
	}
}