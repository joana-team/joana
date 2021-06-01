import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class MethodConstantReturn {

	public static void main(String[] args) {

		MethodConstantReturn c = new MethodConstantReturn();
		c.f(0);
	}

	public int f(int h) {
		int l = -1;
		l = g(h);
		Out.print(l);
		return 0;
	}

	public int g(int x) {
		int y = x + 1;
		y++;
		return x | 1;
	}
}