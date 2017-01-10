/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.annotations.cause;

import edu.kit.joana.api.SPos;
import edu.kit.joana.ui.annotations.Source;

/**
 * TODO: @author Add your name here.
 */
public class JavaSourceAnnotation extends JavaSourceSinkAnnotation<Source>{

	public JavaSourceAnnotation(Source source, String sourceFile) {
		super(
			Source.class,
			new SPos(
				sourceFile,
				source.lineNumber(),
				source.lineNumber(),
				source.columnNumber(),
				source.columnNumber() + Source.class.getSimpleName().length() + 1
		));
	}
}
