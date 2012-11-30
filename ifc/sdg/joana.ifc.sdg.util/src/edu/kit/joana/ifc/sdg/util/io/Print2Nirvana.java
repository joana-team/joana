/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util.io;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Print2Nirvana extends PrintStream {

	public Print2Nirvana() {
		super(new ByteArrayOutputStream());
	}

	@Override
	public void write(byte[] buf, int off, int len) {
		// do nothing!
	}
}
