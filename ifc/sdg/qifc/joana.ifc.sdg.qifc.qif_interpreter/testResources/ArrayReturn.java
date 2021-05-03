import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class ArrayReturn {

	public  static void main(String[] args) {

		ArrayReturn c = new ArrayReturn();
		c.f(0);

	}

	public int f(int h) {
		int[] l = g(h);
		Out.print(l[0]);
		return 0;
	}

	public int[] g(int x) {
		int[] a = new int[1];
		if (x < 0) {
			a[0] = 1;
			return a;
		}
		a[0] = 2;
		return a;
	}
}

