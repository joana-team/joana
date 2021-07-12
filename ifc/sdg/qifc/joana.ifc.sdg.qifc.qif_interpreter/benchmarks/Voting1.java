import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Voting1 {

	public static void main(String[] args) {
		new Voting1().voting(0);
	}

	void voting(int h) {
		int voterCount = 15;

		int result = 0;

		int votes = h & (-1 >>> (32 - voterCount));

		for (int i = 0; i < voterCount; ++i) {
			if (((votes >>> i) & 1) != 0) {
				++result;
			}
		}

		Out.print(result);
	}

}