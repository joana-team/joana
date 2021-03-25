import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class WhileAfterIf {

	public  static void main(String[] args) {

		WhileAfterIf if_ = new WhileAfterIf();
		if_.f(0);

	}

	public int f(int h) {
		int l = 0;
		if (h > 1) {
			l = 1;
		}
		while (h > 0) {
			l++;
			h--;
		}
		Out.print(l);
		return l;
	}
}