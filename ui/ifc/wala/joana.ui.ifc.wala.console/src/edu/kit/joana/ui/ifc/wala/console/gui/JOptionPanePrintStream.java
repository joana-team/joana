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
package edu.kit.joana.ui.ifc.wala.console.gui;

import java.awt.Component;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.swing.JOptionPane;

public class JOptionPanePrintStream extends PrintStream {

	public enum TYPE {
		INFO, WARNING, ERROR;

		int getJOptionPaneCode() {
			switch (this) {
			case INFO:
				return JOptionPane.INFORMATION_MESSAGE;
			case WARNING:
				return JOptionPane.WARNING_MESSAGE;
			case ERROR:
				return JOptionPane.ERROR_MESSAGE;
			default:
				throw new IllegalStateException();
			}
		}

		public String toString() {
			switch (this) {
			case INFO:
				return "Information";
			case WARNING:
				return "Warning";
			case ERROR:
				return "Error";
			default:
				throw new IllegalStateException();
			}
		}
	}

	private TYPE type;
	private Component parent;

	public JOptionPanePrintStream(Component parent, TYPE type) {
		super(new ByteArrayOutputStream());
		this.type = type;
		this.parent = parent;
	}

	@Override
	public void write(byte[] buf, int off, int len) {
		if (len > 1)
			JOptionPane.showMessageDialog(parent, new String(buf, off, len),
					type.toString(), type.getJOptionPaneCode());
	}
}
