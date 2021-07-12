import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class SumAndProduct {

	public static void main(String[] args) {
		new SumAndProduct().sumAndProduct(0, 0);
	}

	void sumAndProduct(int h1, int h2) {
		int sum = 0;
		int product = 1;

		for (int i = 0; i < h1; ++i) {
			sum += h2;
			product *= h2;
		}

		Out.print(sum);
	}

}