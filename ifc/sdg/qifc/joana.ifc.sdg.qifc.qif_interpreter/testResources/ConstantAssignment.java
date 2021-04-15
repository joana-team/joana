import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class ConstantAssignment {

	public  static void main(String[] args) {

		ConstantAssignment l = new ConstantAssignment();
		l.f(0);

	}

	public int f(int h) {
		int l1 = 0;
		while(l1 >= 0) {
			l1 = l1 + 1;
			l1 = -1;
		}
		Out.print(l1);
		return l1;
	}

}