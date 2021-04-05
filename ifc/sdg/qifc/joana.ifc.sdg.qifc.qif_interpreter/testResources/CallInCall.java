import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class CallInCall {

	public  static void main(String[] args) {

		CallInCall c = new CallInCall();
		c.f(0);

	}

	public int f(int h) {
		int l = g(h);
		Out.print(l);
		return 0;
	}

	public int g(int x) {
		if (x < 0) return -1;
		else return h(x);
	}

	public int h(int x) {
		return 0;
	}
}