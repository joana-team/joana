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

import static edu.kit.joana.api.annotations.Annotations.*;

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
		pub.value=0;
		/**
		 * The references transferred to Demo1 are dependent on SECRET.
		 * In particular, the information if the respective secrets are null
		 * may depend on SECRET. Every time a field is read from one of the sec*
		 * attributes of Demo1, a NullPointerException may happen. As the sec*
		 * attributes depend on SECRET, the occurrences of NullPointerExceptions
		 * also depend on SECRET. The occurrence of a NullPointerException in turn
		 * influences the mere occurrence of subsequent events, so everything which
		 * happens after the first read access to a field of a sec* attribute, also
		 * is influenced by SECRET.
		 * So, there may be an information leakage even if toggle() does not do anything
		 * because the call to leak() may or may not happen depending on whether
		 * sec1 is null, which in turn is potentially influenced by SECRET.
		 * Subsequent calls of leak() also are, because they do not happen if sec1 is
		 * null (the whole program simply crashes), influenced by SECRET.
		 * Maybe, there will be a really precise Exception analysis in the future (sometime
		 * after Dec 2012), which finds out, that independent of SECRET, no NullPointerException
		 * will ever happen.
		 */
		pub.value = toggle(sec1.value1) + 28;
		//leak(pub.value);
	}

	public void leak2(int x) {
		pub.value=0;
		if (toggle(sec1.value2) * x * x - 28 * x + 74 == 17) {
			pub.value = 0;
		} else {
			pub.value = 1;
		}
		//leak(pub.value);
	}

	public void leak3() {
		pub.value=0;
		if (toggle(sec2 instanceof SecretB)) {
			pub.value = 0;
		} else {
			pub.value = 1;
		}
		leak(pub.value);
	}


	public static void main(String[] args) {
		Secret sec1 = new Secret(17, 0);
		Secret sec2 = new SecretB();
		Demo1 d = new Demo1(SECRET>0?sec2:sec1, SECRET>0?sec1:sec2);
		d.leak1();
		d.leak2(17);
		d.leak3();
	}
}
