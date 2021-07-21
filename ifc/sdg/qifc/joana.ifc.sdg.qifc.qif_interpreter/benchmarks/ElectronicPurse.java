import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class ElectronicPurse {

	public static void main(String[] args) {
		new ElectronicPurse().f(0);
	}

	public void f(int h) {
		int l = 0;
		while (h >= 5 && h < 20) {
			h = h - 5;
			l = l + 1;
		}
		Out.print(l);
	}
}