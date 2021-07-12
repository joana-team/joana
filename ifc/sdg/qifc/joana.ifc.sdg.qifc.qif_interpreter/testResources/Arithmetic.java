import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

// channel capacity 0
// output value always 2

public class Arithmetic {

	public static void main(String[] args) {

		Arithmetic a = new Arithmetic();
		a.f(1, 0);

	}

	public int f(int h1, int h2) {
		int l = 2;
		h1 = l + h2;
		int x = h1 & 0;
		h2++;
		l = h1 - h2;
		l = x + 2;
		Out.print(l); // 2
		return 0;
	}
}