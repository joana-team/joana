import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class ArrayLoop {

	public  static void main(String[] args) {

		ArrayLoop a = new ArrayLoop();
		a.f(1);

	}

	public int f(int h) {
		int[] a = new int[1];
		a[0] = h;
		int i = 0;
		while (i <= 3) {
			a[0] = ~a[0];
			i++;
		}
		Out.print(a[0]);
		return h;
	}

}