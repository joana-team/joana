/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;

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
	
	public static List<IPath> findProjectBinPaths(final IJavaProject jp) throws JavaModelException {
		final List<IPath> paths = new LinkedList<IPath>();
		final IPath defaultOutputLocation = jp.getOutputLocation();

		try {
			final IClasspathEntry cpentries[] = jp.getResolvedClasspath(true);
			for (final IClasspathEntry cp : cpentries) {
				if (cp.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					final IPath outputLocation = cp.getOutputLocation(); 
					paths.add(outputLocation != null ? outputLocation : defaultOutputLocation);
				}
			}
		} catch (JavaModelException e) {}

		return paths;
	}

	private final static Collection<String> standardLibraryContainersToIgnore = Arrays.asList(
		new String[] { "org.eclipse.jdt.launching.JRE_CONTAINER" }
	);
	
	@SuppressWarnings("restriction")
	private static void addAll(IClasspathEntry raw, JavaProject project, List<IPath> jars) throws JavaModelException {
		IClasspathEntry[] resolvedEntries = project.resolveClasspath(new IClasspathEntry[] { raw });
		for (final IClasspathEntry resolvedEntry : resolvedEntries) {
			if (resolvedEntry.getEntryKind() != IClasspathEntry.CPE_LIBRARY && 
			    resolvedEntry.getEntryKind() != IClasspathEntry.CPE_PROJECT
			) {
				throw new IllegalArgumentException();
			}
			if (resolvedEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
				final IWorkspaceRoot workspaceRoot = project.getProject().getWorkspace().getRoot();
				final IResource resource = workspaceRoot.findMember(resolvedEntry.getPath());
				final IJavaProject other = JavaCore.create((IProject) resource);
				final IPath defaultOutputLocation = other.getOutputLocation();
				for (final IClasspathEntry cp : other.getRawClasspath()) {
					if (cp.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						final IPath outputLocation = cp.getOutputLocation(); 
						jars.add(outputLocation != null ? outputLocation : defaultOutputLocation);							}
				}
			
			} else {
				jars.add(resolvedEntry.getPath());
			}
		}
	}
	
	@SuppressWarnings("restriction")
	public static List<IPath> findProjectJarsExcludingStandardLibries(final IJavaProject jp) {
		final List<IPath> jars = new LinkedList<IPath>();
		
		if (! (jp instanceof JavaProject)) return jars;
		final JavaProject project = (JavaProject) jp;
		

		try {
			final IClasspathEntry rawEntries[] = project.getRawClasspath();
			for (IClasspathEntry entry : rawEntries) {
				switch (entry.getEntryKind()) {
					case IClasspathEntry.CPE_SOURCE:
						break;
					case IClasspathEntry.CPE_CONTAINER:
						if (!standardLibraryContainersToIgnore.contains(entry.getPath().segment(0))) {
							addAll(entry, project, jars);
						}
						break;
					case IClasspathEntry.CPE_PROJECT:
						final IWorkspaceRoot workspaceRoot = project.getProject().getWorkspace().getRoot();
						final IResource resource = workspaceRoot.findMember(entry.getPath());
						final IJavaProject other = JavaCore.create((IProject) resource);
						final IPath defaultOutputLocation = other.getOutputLocation();
						for (final IClasspathEntry cp : other.getRawClasspath()) {
							if (cp.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
								final IPath outputLocation = cp.getOutputLocation(); 
								jars.add(outputLocation != null ? outputLocation : defaultOutputLocation);							}
						}
						break;
					case IClasspathEntry.CPE_LIBRARY:
					case IClasspathEntry.CPE_VARIABLE:
						addAll(entry, project, jars);
						break;
					default:
						throw new IllegalArgumentException();
				}
			}
		} catch (JavaModelException e) {}

		return jars;
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
