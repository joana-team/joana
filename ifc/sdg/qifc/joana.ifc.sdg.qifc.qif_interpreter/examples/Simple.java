import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;
import edu.kit.joana.ui.annotations.Level;
import edu.kit.joana.ui.annotations.Source;

public class Simple {


	public  static void main(String[] args) {

		Simple simple = new Simple();
		simple.f(1, 2);

	}

	public int f(@Source(level=Level.HIGH) int h, @Source(level=Level.LOW) int l1) {
		l1 = ~l1;
		return 0;
	}
}