import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class BreakWithArray {

	public static void main(String[] args) {

		BreakWithArray a = new BreakWithArray();
		a.f(1);

	}

	public int f(int h) {
		int l = 0;
		int[] a = new int[3];
		while (l >= 0) {
			a[l] = 1;
			if (l == 2) {
				break;
			}
			l++;
		}
		Out.print(l);
		return l;
	}

}