import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class x_DeadRecursiveId {

	public static void main(String[] args) {
		new x_DeadRecursiveId().f(0);
	}

	int f(int h) {
		Out.print(id(h));
		return 0;
	}

	int id(int i) {
		int r = 0;
		if (i > 0) {
			r = id(i - 1) + 1;
		}
		return 0;
	}
}