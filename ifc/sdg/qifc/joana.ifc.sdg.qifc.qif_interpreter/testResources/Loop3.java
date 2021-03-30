import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class Loop3 {

	public  static void main(String[] args) {

		Loop3 l = new Loop3();
		l.f(0);

	}

	public int f(int h) {
		int l = 0;
		int k = 0;
		while(3 > l) {
			l++;
			k = k + l;
		}
		Out.print(k);
		return l;
	}

}