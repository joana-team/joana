import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class ArrayLength {

	public  static void main(String[] args) {

		ArrayLength a = new ArrayLength();
		a.f(1);

	}

	public int f(int h) {
		int[] a = new int[2];
		Out.print(a.length);
		return 0;
	}

}