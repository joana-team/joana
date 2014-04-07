/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package prune;

import javax.swing.JTextField;

import sensitivity.Security;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class TestGuiPrune {

	private static String data = (Security.SECRET > 0 ? "Hello World!" : "FooBar");
	
	private JTextField field;
	
	public static void main(String[] args) {
		TestGuiPrune tp = new TestGuiPrune(data);
		String txt = tp.field.getText();
		Security.leak(txt.indexOf('o'));
	}

	private TestGuiPrune(String data) {
		field = new JTextField(data);
	}
}
