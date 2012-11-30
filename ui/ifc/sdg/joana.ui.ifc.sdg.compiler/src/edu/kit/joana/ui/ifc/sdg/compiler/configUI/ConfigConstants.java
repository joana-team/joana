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
package edu.kit.joana.ui.ifc.sdg.compiler.configUI;


import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.IPreferenceStore;

import edu.kit.joana.ui.ifc.sdg.compiler.util.Activator;

/**
 * Configuration constants in the plugins preference store for accessing
 * persisted settings.
 *
 */
public class ConfigConstants {
	private static final String PREFIX = Activator.PLUGIN_ID;

	private static final String COMPILER_PREFIX = PREFIX + ".compiler";

	/**
	 * Constant for accessing the -Xalloprivaccess compiler setting
	 */
	public static final String COMPILER_PRIVATE_ACCESS = COMPILER_PREFIX + ".AllowPrivateAccess";

	/**
	 * Constant for accessing the -Xallowuncaught compiler setting
	 */
	public static final String COMPILER_ALLOW_UNCAUGHT = COMPILER_PREFIX + ".AllowUncaught";

	/**
	 * Constant for determining if project specific settings are to be used
	 */
	public static final String COMPILER_PROJECT_SPECIFIC = COMPILER_PREFIX + ".ProjectSpecific";

	/**
	 * Sets the default values for the config constants in a preference store.
	 *
	 * @param store
	 *            the preference store so set the default values in.
	 */
	public static void setCompilerDefaults(IPreferenceStore store) {
		store.setDefault(ConfigConstants.COMPILER_PRIVATE_ACCESS, false);
		store.setDefault(ConfigConstants.COMPILER_ALLOW_UNCAUGHT, false);
	}

	/**
	 * Returns the <code>IPreferenceStore</code> to be used for retrieving a
	 * <code>IProject</code>'s JOANA compiler settings.
	 *
	 * @param project
	 *            the project for which to return the
	 *            <code>IPreferenceStore</code> to be used.
	 * @return the the <code>IPreferenceStore</code> to be used for retrieving
	 *         a <code>IProject</code>'s JOANA compiler settings.
	 */
	public static IPreferenceStore getUsedPreferenceStore(IProject project) {
		IPreferenceStore projectPrefStore = new ProjectPreferenceStore(project, Activator.PLUGIN_ID);
		if (!projectPrefStore.getBoolean(COMPILER_PROJECT_SPECIFIC))
			return Activator.getDefault().getPreferenceStore();
		return projectPrefStore;
	}
}
