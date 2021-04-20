import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class SimpleArray {

	public  static void main(String[] args) {

		SimpleArray a = new SimpleArray();
		a.f(1);

	}

	public int f(int h) {
		int[] a = new int[1];
		a[0] = h;
		Out.print(a[0]);
		return 0;
	}

}