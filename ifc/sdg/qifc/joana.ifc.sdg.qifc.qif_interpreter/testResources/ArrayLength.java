import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Array {

	public  static void main(String[] args) {

		Array a = new Array();
		a.f(1);

	}

	public int f(int h) {
		int[] a;
		if (h == 1) {
			a = new int[1];
		} else if (h == 2) {
			a = new int[2];
		} else if (h == 3) {
			a = new int[3];
		} else {
			a = new int[0];
		}
		Out.print(a.length);
	}

}