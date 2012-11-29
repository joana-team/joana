/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.demo;


class Secret {
	int value1;
	int value2;

	public Secret(int value1, int value2) {
		this.value1 = value1;
		this.value2 = value2;
	}
}

class SecretA extends Secret {

	public SecretA() {
		super(42, 17);
	}
}

class SecretB extends Secret {

	public SecretB() {
		super(17, 23);
	}
}


class PublicAccessible {
	int value;
}

public class Demo1 {

	Secret sec1;
	Secret sec2;
	PublicAccessible pub;

	public Demo1(Secret sec1, Secret sec2) {
		this.sec1 = sec1;
		this.sec2 = sec2;
	}

	public void leak1() {
		pub.value = sec1.value1 + 28;
	}

	public void leak2(int x) {
		if (sec1.value2 * x * x - 28 * x + 74 == 17) {
			pub.value = 0;
		} else {
			pub.value = 1;
		}
	}

	public void leak3() {
		if (sec2 instanceof SecretA) {
			pub.value = 0;
		} else {
			pub.value = 1;
		}
	}


	public static void main(String[] args) {
		Secret sec1 = new Secret(17, 0);
		Secret sec2 = new SecretB();
		Demo1 d = new Demo1(sec1, sec2);
		d.leak1();
		d.leak2(17);
		d.leak3();
	}
}
