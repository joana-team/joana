/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;

/**
 * @author Martin Mohr
 */
public class ExampleLeakage {
	
	int x;
	
	int getSecret() {
		return 42;
	}
	
	void printValue() {
		System.out.println(x);
	}
	
	void doSomething() {
		x = getSecret();
		printValue();
	}
	
	void doSomethingConditional() {
		x = getSecret();
		if (x > 42) {
			System.out.println("Hallo, Welt!");
		}
	}
	
	
	public static void main(String[] args) {
		ExampleLeakage e = new ExampleLeakage();
		e.doSomething();
		e.doSomethingConditional();
	}
}
