import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class IdenticalBranches {

	public  static void main(String[] args) {

		IdenticalBranches if_ = new IdenticalBranches();
		if_.f(0);

	}

	public int f(int h) {
		int l = 0;
		if (h > 0) {
			l = 1 + l;
		} else {
			l = l + 1;
		}
		Out.print(l);
		return l;
	}
}