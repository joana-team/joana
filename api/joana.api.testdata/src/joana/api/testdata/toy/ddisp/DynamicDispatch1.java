/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.ddisp;

/**
 * @author Martin Mohr
 */
public class DynamicDispatch1 {
	
	public static void main(String[] args) {
		realMain("Hello");
	}
	
	private static void realMain(String arg0) {
		BaseClass b;
		if (arg0.length() < 42) {
			b = new BaseClass();
		} else {
			b = new Malicious();
		}
		
		b.leak();
	}
	
}

class BaseClass {
	void leak() {}
}

class Malicious extends BaseClass {
	void leak() {
		System.out.println("Hello, World!");
	}
}
