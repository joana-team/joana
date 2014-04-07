/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package prune;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JTextField;

import sensitivity.Security;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class TestGuiPruneExtended extends JFrame {

	private static String data = (Security.SECRET > 0 ? "Hello World!" : "FooBar");
	
	private JTextField field;
	
	public static void main(String[] args) {
		new TestGuiPruneExtended(data);
	}

	private TestGuiPruneExtended(String data) {
		field = new JTextField(data);
		this.add(field);
		this.pack();
		this.setVisible(true);
		this.addWindowListener(new ExitListener(this));
	}
	
	private static class ExitListener extends WindowAdapter {
		
		private final TestGuiPruneExtended parent;
		
		private ExitListener(final TestGuiPruneExtended parent) {
			this.parent = parent;
		}
		
		@Override
		public void windowClosing(WindowEvent event) {
			String txt = parent.field.getText();
			Security.leak(txt.indexOf('o'));
			System.exit(0);
		}
	}
}
