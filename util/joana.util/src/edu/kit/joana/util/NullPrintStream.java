/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * PrintStream that doesn't print anything
 */
public class NullPrintStream extends PrintStream {

	public NullPrintStream() {
		super(new OutputStream() {
			
			@Override
			public void write(int b) throws IOException {
			}
		});
	}

}
