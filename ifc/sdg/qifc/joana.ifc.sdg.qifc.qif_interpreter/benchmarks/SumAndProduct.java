import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class SumAndProduct {

	public static void main(String[] args) {
		new SumAndProduct().sumAndProduct(0, 0);
	}

	void sumAndProduct(int h1, int h2) {
		int sum = h2;
		int product = 0;
		int i = 0;

		while (i != h1) {
			sum += 1;
			product += h2;
			i++;
		}

		Out.print(sum);
	}

}