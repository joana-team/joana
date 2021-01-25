public class Or {

	public  static void main(String[] args) {

		Or or = new Or();
		or.f(1, 1);

	}

	public int f(int h1, int h2) {
		int l = 2;
		l = l | h1;
		h1 = h1 | h1;
		h2 = h1 | h2;
		return l;
	}

}