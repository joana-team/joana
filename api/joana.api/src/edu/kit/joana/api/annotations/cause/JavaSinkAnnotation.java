/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.annotations.cause;

import edu.kit.joana.api.SPos;
import edu.kit.joana.ui.annotations.Sink;

/**
 * TODO: @author Add your name here.
 */
public class JavaSinkAnnotation extends JavaSourceSinkAnnotation<Sink>{

	public JavaSinkAnnotation(Sink sink, String sourceFile) {
		super(
			Sink.class,
			new SPos(
				sourceFile,
				sink.lineNumber(),
				sink.lineNumber(),
				sink.columnNumber(),
				sink.columnNumber() + Sink.class.getSimpleName().length() + 1
		));
	}
}
