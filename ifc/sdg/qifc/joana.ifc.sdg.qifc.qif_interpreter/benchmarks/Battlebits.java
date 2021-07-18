import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Battlebits {

	public static void main(String[] args) {
		new Battlebits().battleBits(0);
	}

	void battleBits(int board) {

		int hits = 0;

		int[] shots = new int[3];
		shots[0] = 0;
		shots[1] = 2;
		shots[2] = 5;

		for (int i = 0; i != 3; ++i) {
			if (((board >> shots[i]) & 1) != 0) {
				hits++;
			}
		}
		Out.print(hits);
	}
}