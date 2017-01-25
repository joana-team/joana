/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.test;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.SECRET_STRING;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.leak;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.toggle;

class VerySecretString {
	String value;

	public VerySecretString(String value) {
		this.value = value;
	}
}


class VeryPublicObject {
	boolean state;

	public void analyze(VerySecretString s) {
		if (toggle(s.value == null)) {
			state = true;
		} else {
			state = false;
		}
	}
}

public class VeryImplictFlow {


	public static void main(String[] args) {
		VerySecretString s1 = new VerySecretString(SECRET_STRING);
		VerySecretString s2 = new VerySecretString(SECRET_STRING);
		VeryPublicObject pub = new VeryPublicObject();
		pub.analyze(s1);
		leak(pub.state);
		pub.analyze(s2);
		leak(pub.state);
	}
}
