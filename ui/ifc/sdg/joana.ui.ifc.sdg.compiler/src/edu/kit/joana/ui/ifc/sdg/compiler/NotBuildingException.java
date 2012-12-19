/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler;

/**
 * Thrown if the compilation state of a file was requested that is not part of
 * an IFC build process.
 *
 */
public class NotBuildingException extends RuntimeException {

	private static final long serialVersionUID = -8606741142875627330L;

}
