import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class NotQuiteIdenticalBranches {

	public  static void main(String[] args) {

		NotQuiteIdenticalBranches if_ = new NotQuiteIdenticalBranches();
		if_.f(0);

	}

	public int f(int h) {
		int l = 0;
		if (h > 0) {
			l = l;
		} else {
			l = l + 1;
		}
		Out.print(l);
		return l;
	}
}