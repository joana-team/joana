/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.demo;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.SECRET;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.leak;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.toggle;

public class NonNullFieldParameter {
	Secret sec;

	public NonNullFieldParameter(Secret sec) {
		this.sec = sec;
	}

	public static void main(String[] args) {
		Secret sec1 = new Secret(1);
		Secret sec2 = new Secret(2);
		NonNullFieldParameter d = new NonNullFieldParameter(SECRET>0?sec1:sec2);
		/**
		 * d.sec depends on SECRET. However, if toggle ignores its parameter,
		 * no direct leak exists. The only reason JOANA finds a leak in this case
		 * is because of exceptional control flow. The parameter given at d's
		 * constructor cannot be null (as the NullPointerAnalysis finds out!),
		 * so d.sec will not be null at this point. However, the NullPointerAnalysis
		 * does not properly tracks this fact through field set and read, and thinks that
		 * d.sec might be null. Since d.sec depends on SECRET, it generates a false alarm.
		 * If we use (SECRET>0?sec1:sec2) directly, JOANA is precise enough.
		 * 
		 * Maybe, there will be a really precise Exception analysis in the future (sometime
		 * after Dec 2012), which finds out, that independent of SECRET, no NullPointerException
		 * will ever happen.
		 */
		int x = toggle(d.sec.value) + 28;
		leak(x);
	}
	
	static class Secret {
		int value;

		public Secret(int value) {
			this.value = value;
		}
	}
}