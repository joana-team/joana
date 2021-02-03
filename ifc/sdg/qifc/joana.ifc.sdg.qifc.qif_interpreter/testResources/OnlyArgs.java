import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class OnlyArgs {

	public  static void main(String[] args) {

		OnlyArgs a = new OnlyArgs();
		a.f(1, 0);

	}

	public int f(int h1, int h2) {
		Out.print(h1); // 2
		Out.print(h2); // 1
		return 0;
	}
}