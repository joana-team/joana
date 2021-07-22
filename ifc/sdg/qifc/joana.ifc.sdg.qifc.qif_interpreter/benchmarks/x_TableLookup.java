import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class x_TableLookup {

	public static void main(String[] args) {
		new x_TableLookup().lookup(0);
	}

	public void lookup(int h) {
		int[] table = new int[8];
		table[0] = 0;
		table[1] = 1;
		table[2] = 2;
		table[3] = 3;
		table[4] = 4;
		table[5] = 5;
		table[6] = 6;
		table[7] = 7;

		int l = 0;

		if (0 <= h && h < 8) {
			l = table[h];
		} else {
			l = 0;
		}
		Out.print(l);
	}
}