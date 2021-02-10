import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Loop {

	public  static void main(String[] args) {

		Loop l = new Loop();
		l.f(0);

	}

	public int f(int h) {
		int l = 0;
		while(h > 0) {
			l++;
			h--;
		}
		Out.print(l);
		return l;
	}

}