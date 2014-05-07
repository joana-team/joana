/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.*;

/**
 * @author Martin Mohr
 */
public class ExampleLeakage {
	
	int x;
	
	int getSecret() {
		return SECRET;
	}
	
	void printValue() {
		leak(x);
	}
	
	void doSomething() {
		x = toggle(getSecret());
		printValue();
	}
	
	void doSomethingConditional() {
		x = toggle(getSecret());
		if (x > 42) {
			leak("Hallo, Welt!");
		}
	}
	
	
	public static void main(String[] args) {
		ExampleLeakage e = new ExampleLeakage();
		e.doSomething();
		e.doSomethingConditional();
	}
}
