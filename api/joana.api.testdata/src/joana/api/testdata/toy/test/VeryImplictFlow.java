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
package joana.api.testdata.toy.test;

class VerySecretString {
	String value;

	public VerySecretString(String value) {
		this.value = value;
	}
}


class VeryPublicObject {
	boolean state;

	public void analyze(VerySecretString s) {
		if (s.value.length() % 2 == 0) {
			state = true;
		} else {
			state = false;
		}
	}
}

public class VeryImplictFlow {


	public static void main(String[] args) {
		VerySecretString v1 = new VerySecretString("Hallo");
		VerySecretString v2 = new VerySecretString("Hallo!");
		VeryPublicObject pub = new VeryPublicObject();
		pub.analyze(v1);
		pub.analyze(v2);
	}
}
