/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.declass;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.*;

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
		Object o = SECRET_BOOL ? new Secret() : new Secret();
		Secret s = (Secret) o;
		
		/**
		 * Our analysis does not see that because "o" is always an instance of Secret,
		 * the cast always succeeds.
		 * So JOANA thinks everything is dependent on SECRET_OBJECT
		 * even if it is not the case.
		 */
		Secret s2 = declass(toggle(s));
		bar(s2);
	}
}
