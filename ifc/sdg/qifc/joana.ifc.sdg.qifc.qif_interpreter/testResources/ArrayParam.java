import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class ArrayParam {

	public  static void main(String[] args) {

		ArrayParam c = new ArrayParam();
		c.f(0);

	}

	public int f(int h) {
		int[] a = new int[1];
		a[0] = 1;
		int l = add(a, h);
		Out.print(l);
		return 0;
	}

	public int add(int[] x, int y) {
		return x[0] + y;
	}
}

