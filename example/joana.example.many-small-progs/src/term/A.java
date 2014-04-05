/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package term;

import sensitivity.Security;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class A {

	public void start1() {
		recursion(four());
	}

	public void start2() {
		recurs1();
	}

	public void start3() {
		four();
	}

	public void start4() {
		start1();
	}

	public void start5() {
		endless();
	}

	public int four() {
		return 4;
	}


	public int recursion(int x) {
		if (x > 3) {
			x = recursion(x / 2);
			x++;
		}

		return x;
	}

	public int recurs1() {
		return recurs2();
	}

	public int recurs2() {
		return recurs1();
	}

	public int endless() {
		int x = 4;

		while (x < 5) {
		}
		x++;

		return x;
	}
	
	public static void main(String[] args) {
		A a = new A();
		
		switch (Security.SECRET) {
		case 1: a.start1();
			Security.leak(23);
			break;
		case 2: a.start2();
			Security.leak(23);
			break;
		case 3: a.start3();
			Security.leak(23);
			break;
		case 4: a.start4();
			Security.leak(23);
			break;
		case 5: a.start5();
			Security.leak(23);
			break;
		case 6: a.four();
			Security.leak(23);
			break;
		case 7: a.recursion(5);
			Security.leak(23);
			break;
		case 8: a.recurs1();
			Security.leak(23);
			break;
		case 9: a.recurs2();
			Security.leak(23);
			break;
		case 10: a.endless();
			Security.leak(23);
			break;
		}
		
		Security.leak(23);
	}
}
