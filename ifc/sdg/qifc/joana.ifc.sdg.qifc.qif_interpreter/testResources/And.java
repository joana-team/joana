import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

// channel capacity: maximum --> every possible value can appear as output
public class And {

	public  static void main(String[] args) {

		And and = new And();
		and.f(1, 0);

	}

	public int f(int h1, int h2) {
		int l = h1 & h2;
		Out.print(l);
		return l;
	}

}