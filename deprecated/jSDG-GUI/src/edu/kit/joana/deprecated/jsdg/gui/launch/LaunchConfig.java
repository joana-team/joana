/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.gui.launch;

import edu.kit.joana.deprecated.jsdg.SDGFactory;
import edu.kit.joana.deprecated.jsdg.util.Log.LogLevel;

public final class LaunchConfig {

	public static final String PROJECT_NAME = "PROJECT_NAME";
	public static final String PROJECT_NAME_DEFAULT = "";

	public static final String MAIN_CLASS_NAME = "MAIN_CLASS_NAME";
	public static final String MAIN_CLASS_NAME_DEFAULT = "";

	public static final String EXCLUSIONS = "EXCLUSIONS";
	public static final String NEW_LINE = System.getProperty("line.separator");
	public static final String EXCLUSIONS_DEFAULT =
		"java/awt/.*" + NEW_LINE +
///		"java/security/.*" + NEW_LINE +
		"javax/swing/.*" + NEW_LINE +
		"sun/awt/.*" + NEW_LINE +
		"sun/swing/.*" + NEW_LINE +
		"com/sun/.*" + NEW_LINE +
		"sun/.*";
	public static final String EXCLUSIONS_INVERT = "EXCLUSIONS_INVERT";
	public static final Boolean EXCLUSIONS_INVERT_DEFAULT = false;

	public static final String NON_TERMINATION = "NON_TERMINATION";
	public static final Boolean NON_TERMINATION_DEFAULT = false;

	public static final String IGNORE_EXCEPTIONS = "IGNORE_EXCEPTIONS";
	public static final Boolean IGNORE_EXCEPTIONS_DEFAULT = false;

	public static final String OPTIMIZE_EXCEPTIONS = "OPTIMIZE_EXCEPTIONS";
	public static final Boolean OPTIMIZE_EXCEPTIONS_DEFAULT = true;

	public static final String ADD_CONTROL_FLOW = "ADD_CONTROL_FLOW";
	public static final Boolean ADD_CONTROL_FLOW_DEFAULT = false;

	public static final String SIMPLE_DATA_DEP = "SIMPLE_DATA_DEP";
	public static final Boolean SIMPLE_DATA_DEP_DEFAULT = false;

	public static final String COMPUTE_INTERFERENCE = "COMPUTE_INTERFERENCE";
	public static final Boolean COMPUTE_INTERFERENCE_DEFAULT = false;

	public static final String POINTS_TO_TYPE = "POINTS_TO_TYPE";
	public static final String POINTS_TO_TYPE_DEFAULT =
		SDGFactory.Config.PointsToType.VANILLA_ZERO_ONE_CFA.toString();

	public static final String LOG_LEVEL = "LOG_LEVEL";
	public static final String LOG_LEVEL_DEFAULT = LogLevel.INFO.toString();

	public static final String SCOPE_FILE_DATA = "SCOPE_FILE_DATA";
	public static final String SCOPE_FILE_DATA_DEFAULT =
		"Primordial,Java,stdlib,none" + NEW_LINE +
		"Primordial,Java,jarFile,/afs/info.uni-karlsruhe.de/user/grafj/workspaces/workspace-3.3/jSDG/lib/stubs.jar" + NEW_LINE +
		"Primordial,Java,jarFile,/afs/info.uni-karlsruhe.de/user/grafj/workspaces/workspace-3.3/jSDG/lib/primordial.jar.model";

	public static final String LOGFILE = "LOGFILE";
	public static final String LOGFILE_DEFAULT = "";

}
