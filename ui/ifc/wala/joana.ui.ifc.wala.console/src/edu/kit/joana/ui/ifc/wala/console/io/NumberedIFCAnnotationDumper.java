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

import java.io.PrintStream;
import java.util.Collection;

import edu.kit.joana.api.annotations.IFCAnnotation;

public class NumberedIFCAnnotationDumper extends IFCAnnotationDumper {

	public NumberedIFCAnnotationDumper(PrintStream out) {
		super(out);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see main.SDGNodeAnnotationDumper#dumpAnnotations(java.util.Collection)
	 */
	@Override
	public void dumpAnnotations(Collection<IFCAnnotation> annotations) {
		int i = 0;
		for (IFCAnnotation ann : annotations) {
			out.print("[" + i + "] ");
			dumpAnnotation(ann);
			out.println();
			i++;
		}
	}
}
