/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.flowless.util;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

public final class ProjectUtil {

	private ProjectUtil() {}

	public static List<IPath> findProjectSourcePaths(final IJavaProject jp) {
		final List<IPath> paths = new LinkedList<IPath>();

		try {
			final IClasspathEntry cpentries[] = jp.getResolvedClasspath(true);
			for (final IClasspathEntry cp : cpentries) {
				if (cp.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					paths.add(cp.getPath());
				}
			}
		} catch (JavaModelException e) {}

		return paths;
	}

	public static List<IPath> findProjectJars(final IJavaProject jp) {
		final List<IPath> jars = new LinkedList<IPath>();

		try {
			final IClasspathEntry cpentries[] = jp.getResolvedClasspath(true);
			for (final IClasspathEntry cp : cpentries) {
				if (cp.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					jars.add(cp.getPath());
				}
			}
		} catch (JavaModelException e) {}

		return jars;
	}

}
