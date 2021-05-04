import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class ArrayRecursion {

	public static void main(String[] args) {

		ArrayRecursion c = new ArrayRecursion();
		c.f(0);

	}

	public int f(int n) {
		int[] a = new int[3];
		Out.print(rec(a, n, 0)[2]);
		return 0;
	}

	public int[] rec(int[] a, int val, int idx) {
		if (idx == 3) {
			return a;
		} else {
			a[idx] = val;
			return rec(a, val, idx + 1);
		}
	}
}

