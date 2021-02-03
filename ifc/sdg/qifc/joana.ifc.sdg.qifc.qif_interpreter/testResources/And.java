public class And {

	public  static void main(String[] args) {

		And and = new And();
		and.f(1, 0);

	}

	public int f(int h1, int h2) {
		int l = 2;
		l = l & h1;
		h1 = h1 & h1;
		h2 = h1 & h2;
		return l;
	}

}