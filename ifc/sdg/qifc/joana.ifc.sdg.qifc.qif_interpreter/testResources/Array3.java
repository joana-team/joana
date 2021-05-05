import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Array3 {

	public  static void main(String[] args) {

		Array3 a = new Array3();
		a.f(1);

	}

	public int f(int h) {
		int[] a = new int[3];
		int i = 0;
		while (i < 3) {
			a[i] = i;
			i++;
		}

		if (h >= 0 && h < 3) {
			i = h;
		} else {
			i = 0;
		}
		Out.print(a[i]);
		return 0;
	}

}
