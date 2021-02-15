import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Or {

	public  static void main(String[] args) {

		Or or = new Or();
		or.f(1, 0);

	}

	public int f(int h1, int h2) {
		int l = h1 | h2;
		Out.print(l);
		return l;
	}

}