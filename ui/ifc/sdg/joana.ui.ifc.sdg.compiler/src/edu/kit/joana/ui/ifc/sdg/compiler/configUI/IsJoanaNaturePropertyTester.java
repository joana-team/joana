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


import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;

import edu.kit.joana.ui.ifc.sdg.compiler.nature.JoanaNature;
import edu.kit.joana.ui.ifc.sdg.compiler.util.PluginUtil;

/**
 * This property tester implements tests related to the JOANA project nature:
 *
 * isJoanaProject: indicates if the project has the JOANA nature
 *
 */
public class IsJoanaNaturePropertyTester extends PropertyTester {

	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (property.equals("isJoanaProject")) {
			if (!(receiver instanceof IJavaProject))
				return false;
			IJavaProject javaProject = (IJavaProject) receiver;
			try {
				return PluginUtil.hasNature(javaProject.getProject(), JoanaNature.ID);
			} catch (CoreException e) {
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}

}
