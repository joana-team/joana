import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Implicit {

	public  static void main(String[] args) {

		Implicit i = new Implicit();
		i.f(0);

	}

	public int f(int h) {
		int l = 0;
		if (h > 0) {
			l = 1;
		}
		Out.print(l);
		return l;
	}

}