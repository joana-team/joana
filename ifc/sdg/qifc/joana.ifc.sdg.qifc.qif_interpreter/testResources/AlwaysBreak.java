import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class AlwaysBreak {

	public  static void main(String[] args) {

		AlwaysBreak a = new AlwaysBreak();
		a.f(1);

	}

	public int f(int h) {
		int l = 0;
		while (l >= 0) {
			l++;
			break;
		}
		Out.print(l);
		return l;
	}

}