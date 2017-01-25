/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.builder;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;

import edu.kit.joana.ui.ifc.sdg.compiler.util.PDEUtil;
import edu.kit.joana.ui.ifc.sdg.compiler.util.PluginUtil;

/**
 * A JDT ElementChangedListener watching classpath changes to schedule a rebuild
 * of affected projects with the JOANA nature.
 *
 */
public class BuildpathListener implements IElementChangedListener {

	public void elementChanged(ElementChangedEvent event) {
		walkDelta(event.getDelta());
	}

	private void walkDelta(IJavaElementDelta delta) {
		if ((delta.getFlags() & IJavaElementDelta.F_CLASSPATH_CHANGED) != 0)
			handleClasspathChanged(delta.getElement());
		for (IJavaElementDelta child : delta.getAffectedChildren())
			walkDelta(child);
	}

	private void handleClasspathChanged(IJavaElement element) {
		try {
			List<IJavaProject> dependentProjects = PDEUtil.getDependentProjects(element.getJavaProject());
			for (IJavaProject project : dependentProjects)
				if (project.getProject() != element.getJavaProject().getProject())
					if (PluginUtil.hasBuilder(project.getProject(), JoanaBuilder.ID)) {
						JoanaBuilder.scheduleBuild(project.getProject(), true);
					}
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
