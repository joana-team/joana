import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class ArrayInIf {

	public  static void main(String[] args) {

		ArrayInIf if_ = new ArrayInIf();
		if_.f(0);

	}

	public int f(int h) {
		int[] a = new int[1];
		if (h >= 0) {
			a[0] = 1;
		} else {
			a[0] = 0;
		}
		Out.print(a[0]);
		return h;
	}
}