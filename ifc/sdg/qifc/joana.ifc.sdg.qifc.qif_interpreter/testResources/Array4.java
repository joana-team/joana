import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Array4 {

	public  static void main(String[] args) {

		Array4 a = new Array4();
		a.f(1);

	}

	public int f(int h) {
		int[] a = new int[3];
		a[0] = h;
		int l = a[0];
		a[0] = 0;
		Out.print(l);
		return 0;
	}

}
