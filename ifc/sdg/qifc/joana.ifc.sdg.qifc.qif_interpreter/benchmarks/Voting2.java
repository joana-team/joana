import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Voting2 {

	public static void main(String[] args) {
		new Voting2().voting(0, 0, 0);
	}


	void voting(int h1, int h2, int h3) {
		int result = 0;
		int canditateCount = 3;

		for (int i = 0; i != canditateCount; ++i) {
			int count = 0;

			if (h1 == i) {
				++count;
			}

			if (h2 == i) {
				++count;
			}

			if (h3 == i) {
				++count;
			}

			result |= (count << (2 * i));
		}
		Out.print(result);
	}
}