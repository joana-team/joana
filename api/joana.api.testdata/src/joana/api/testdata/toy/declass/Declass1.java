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
package joana.api.testdata.toy.declass;

import static edu.kit.joana.api.annotations.Annotations.*;

class SecretWrapper extends Secret {
	private Secret wrapped;

	public SecretWrapper(Secret wrapped) {
		this.wrapped = wrapped;
	}
}

public class Declass1 {

	static Object pub;

	public static Secret declass(Secret s) {
		return new SecretWrapper(s);
	}

	public static void bar(Object o) {
		leak(o);
	}

	public static void main(String[] args) {
		Secret s = (Secret) SECRET_OBJECT;
		
		/**
		 * Our analysis does not rule out, that SECRET_OBJECT is null. If it is, the cast will fail
		 * and subsequent events do not happen. Therefore, everything is dependent on SECRET_OBJECT
		 * even if it does not seem to be.
		 */
		Secret s2 = declass(toggle(s));
		bar(s2);
	}
}
