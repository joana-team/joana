import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Simple {

	public static void main(String[] args) {

		Simple a = new Simple();
		a.f(1);

	}

	public int f(int h) {
		int l = 1 & h;
		int k = h + l;
		Out.print(k);
		return 0;
	}
}