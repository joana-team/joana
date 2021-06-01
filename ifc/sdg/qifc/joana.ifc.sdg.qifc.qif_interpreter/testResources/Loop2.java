import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Loop2 {

	public  static void main(String[] args) {

		Loop2 l = new Loop2();
		l.f(0);

	}

	public int f(int h) {
		int l = 0;
		while (3 > l) {
			l++;
		}
		Out.print(l);
		return l;
	}

}