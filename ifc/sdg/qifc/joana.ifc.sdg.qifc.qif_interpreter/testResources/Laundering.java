import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Laundering {

	public static void main(String[] args) {

		Laundering l = new Laundering();
		l.f(0);

	}

	public int f(int h) {
		int l = 0;


		while (l != h) {
			l++;
		}


		l = l & 1;
		Out.print(l);



		return l;
	}
}