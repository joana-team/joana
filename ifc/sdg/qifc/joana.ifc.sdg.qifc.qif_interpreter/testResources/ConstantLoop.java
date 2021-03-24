import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class ConstantLoop {

	public  static void main(String[] args) {

		ConstantLoop l = new ConstantLoop();
		l.f(0);

	}

	public int f(int h) {
		int l1 = 0;
		while(l1 == 0) {
			l1 = ~l1;
		}
		l1 = ++l1;
		Out.print(l1);
		return l1;
	}

}