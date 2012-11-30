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
package edu.kit.joana.ui.ifc.wala.console.io;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ConsolePrintStreamWrapper extends PrintStream {

    private final IFCConsoleOutput out;

    public ConsolePrintStreamWrapper(final IFCConsoleOutput out) {
	super(new ByteArrayOutputStream());
	this.out = out;
    }

    @Override
    public void write(byte[] buf, int off, int len) {
	out.log(new String(buf, off, len));
    }

}
