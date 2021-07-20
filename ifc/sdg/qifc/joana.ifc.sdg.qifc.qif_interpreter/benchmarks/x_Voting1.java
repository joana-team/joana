import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class x_Voting1 {

	public static void main(String[] args) {
		new x_Voting1().voting(0);
	}

	void voting(int h) {
		int voterCount = 32;

		int result = 0;

		for (int i = 0; i != voterCount; ++i) {
			if (((h >> i) & 1) != 0) {
				++result;
			}
		}

		Out.print(result);
	}

}