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
package edu.kit.joana.ui.ifc.sdg.compiler;

/**
 * Thrown if the build result of a file could not be returned because the build
 * process has previousely failed.
 *
 */
public class CompilerErrorException extends RuntimeException {

	private static final long serialVersionUID = -197333228499961546L;

	CompilerErrorException(String compilerError) {
		super(compilerError);
	}

}
