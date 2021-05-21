public class Iterations {

	public static void main(String[] args) {

		Iterations a = new Iterations();
		a.f(4);

	}

	public int f(int h) {
		int a = h;
		int z = 1;
		int f = 0b11111111111111111111111111111110;
		int s = 0b11111111111111111111111111111101;
		int t = 0b11111111111111111111111111111011;

		while (a > 4) {
			a = a - 1;
			if (z == 1) {
				h = h & f;
			}
			if (z == 2) {
				h = h & s;
			}
			if (z == 3) {
				h = h & t;
			}
			z = z + 1;
			h = h & 0b11111111111111111111110000000000;
		}
		System.out.println(h);
		return h;
	}
}
