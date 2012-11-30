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
package edu.kit.joana.ui.ifc.sdg.compiler.builder;


import org.eclipse.core.resources.IMarker;

import edu.kit.joana.ui.ifc.sdg.compiler.util.Activator;

/**
 * Constants for handling JOANA build problem markers.
 *
 */
public interface IJoanaBuildProblemMarker {

	/**
	 * The name of the attribute for compiler output
	 */
	public static final String COMPILER_OUTPUT = "compilerOutput";

	/**
	 * The name of the attribute for the executed command line
	 */
	public static final String COMMAND_LINE = "commandLine";

	/**
	 * The qualified name of the JOANA problem marker
	 */
	public static final String ID = Activator.PLUGIN_ID + ".joanaBuildProblemMarker";

	/**
	 * The problem marker severity to be used for JOANA problem markers
	 */
	public static final int PROBLEM_SEVERITY = IMarker.SEVERITY_WARNING;

	/**
	 * The problem marker message to be displayed for JOANA problem markers.
	 */
	public static final String PROBLEM_MESSAGE = "Joana compiler processing error";
}
