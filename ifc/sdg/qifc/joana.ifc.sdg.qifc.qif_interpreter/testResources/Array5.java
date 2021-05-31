import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Array5 {

	public  static void main(String[] args) {

		Array5 a = new Array5();
		a.f(1);

	}

	public int f(int h) {
		int[] a = new int[3];
		int i = 1;
		while (i < 3) {
			a[i] = i & h;
			i++;
		}
		Out.print(a[2]);
		return 0;
	}
}