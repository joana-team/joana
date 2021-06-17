import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Segment {

	public static void main(String[] args) {

		Segment a = new Segment();
		a.f(1);

	}

	public int f(int h) {
		int l = 1 | h;
		int k = 3 | l;
		k = copy(k);
		Out.print(k);
		return 0;
	}

	public int copy(int x) {
		return x;
	}
}