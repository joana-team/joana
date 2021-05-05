import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Array6 {

	public  static void main(String[] args) {

		Array6 a = new Array6();
		a.f(1);

	}

	public int f(int h) {
		int[] a = new int[3];
		int i = 0;
		while (i < 3) {
			a[i] = i;
			i++;
		}

		if (h > 0) {
			i = h - 1;
		} else {
			i = 0;
		}
		Out.print(a[i]);
		return 0;
	}

}

