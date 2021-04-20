import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Array2 {

	public  static void main(String[] args) {

		Array2 a = new Array2();
		a.f(1);

	}

	public int f(int h) {
		int[] a = new int[3];
		a[0] = h + 1;
		int l = a[0];
		a[0] = h + 2;
		int k = a[0] + 1;
		Out.print(k);
		Out.print(l);
		return 0;
	}

}