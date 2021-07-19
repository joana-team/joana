import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class x_Sum {

	public static void main(String[] args) {
		new x_Sum().sum(0, 0, 0);
	}

	public void sum(int h1, int h2, int h3) {
		Out.print(h1 + h2 + h3);
	}
}