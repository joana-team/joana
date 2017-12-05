/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.sensitivity;

import edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks;

/**
 * This program is taken from the SecuriBench suite. It is secure and Joana can detect this,
 * since it applies local killing definitions.
 *
 * @author Martin Mohr
 */
public class StrongUpdate4 {
	private String name;
	public void doGet() {
		this.name = ToyTestsDefaultSourcesAndSinks.SECRET_STRING;
		this.name = "abc";
		ToyTestsDefaultSourcesAndSinks.leak(name);
	}
	public static void main(String[] args) {
		StrongUpdate4 su = new StrongUpdate4();
		su.doGet();
	}
}
