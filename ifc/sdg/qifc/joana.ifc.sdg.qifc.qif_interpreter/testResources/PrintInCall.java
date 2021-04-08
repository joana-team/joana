import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class PrintInCall {

	public  static void main(String[] args) {

		PrintInCall c = new PrintInCall();
		c.f(0);

	}

	public int f(int h) {
		int l = g(h);
		return 0;
	}

	public int g(int x) {
		Out.print(x);
		return 0;
	}
}