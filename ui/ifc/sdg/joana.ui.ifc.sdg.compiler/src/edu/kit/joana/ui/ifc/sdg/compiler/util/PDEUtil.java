/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Various utilities for working with the PDE core plugin
 *
 */
public class PDEUtil {

	/**
	 * Returns a list of all Java projects that have a required Java project in
	 * their dependency tree.
	 *
	 * @param requiredProject
	 *            the required project to get all dependant projects for.
	 * @return a list of all Java projects that have <code>requiredProject</code>
	 *          in their dependency tree.
	 * @throws CoreException
	 *             if the list of dependent projects could not be determinded.
	 */
	public static List<IJavaProject> getDependentProjects(IJavaProject requiredProject) throws CoreException {
		IWorkspaceRoot root = requiredProject.getProject().getWorkspace().getRoot();
		List<IJavaProject> ret = new ArrayList<IJavaProject>();
		for (IProject project : root.getProjects()) {
			IJavaProject javaProject = JavaCore.create(project);
			if (javaProject == null)
				continue;
			if (depends(javaProject, requiredProject))
				ret.add(javaProject);
		}
		return ret;
	}

	/**
	 * Checks if a Java project has a required Java project in its dependency
	 * tree.
	 *
	 * @param javaProject
	 *            the Java project to check the dependency for.
	 * @param requiredProject
	 *            the required Java project.
	 * @return <code>true</code> if <code>javaProject</code> has
	 *         <code>requiredProject</code> in its dependency tree, else
	 *         <code>false</code>
	 * @throws CoreException
	 *             if the dependency could not be determined.
	 */
	public static boolean depends(IJavaProject javaProject, IJavaProject requiredProject) throws CoreException {
		IWorkspaceRoot root = requiredProject.getProject().getWorkspace().getRoot();
		if (javaProject.getProject() == requiredProject.getProject())
			return true;
		for (String reqName : javaProject.getRequiredProjectNames()) {
			IJavaProject directReq = JavaCore.create(root.getProject(reqName));
			if (directReq == null)
				continue;
			if (depends(directReq, requiredProject))
				return true;
		}
		return false;
	}

	/**
	 * Collects a list of Java compiler command line compatible classpath
	 * entries of requirements for a project.
	 *
	 * @param project
	 *            the project to gather the classpath entries for
	 * @return a list of classpath entry strings, or <code>null</code> if
	 *         <code>project</code> is not a Java project.
	 * @throws JavaModelException
	 *             if the classpath entries could not be determined.
	 */
	public static List<String> collectClasspathEntries(IProject project) throws JavaModelException {
		List<String> classpathEntries = new ArrayList<String>();
		IWorkspaceRoot workspaceRoot = project.getWorkspace().getRoot();
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject == null)
			return null;

		// Source and library entries
		for (IClasspathEntry entry : javaProject.getResolvedClasspath(true)) {
			switch (entry.getEntryKind()) {
			case IClasspathEntry.CPE_SOURCE: {
				//String path = PluginUtil.workspaceRelativePath2Filesystem(workspaceRoot, entry.getPath());
				URI uri = null;
				try {
					uri = project.getDescription().getLocationURI();
				} catch (CoreException e) {
					e.printStackTrace();
				}

				String path = null;
				if (uri == null) {
					path = PluginUtil.workspaceRelativePath2Filesystem(workspaceRoot, entry.getPath());
				} else {
					path = uri.getRawPath();
				}

//			    String path = project.getRawLocation().toOSString();
				classpathEntries.add(path);
				break;
			}
			case IClasspathEntry.CPE_LIBRARY: {
				String path = entry.getPath().toOSString();
				if (workspaceRoot.findMember(entry.getPath()) != null)
					path = PluginUtil.workspaceRelativePath2Filesystem(workspaceRoot, entry.getPath());
				classpathEntries.add(path);
				break;
			}
			}
		}

		// Recursive for required projects
		for (String projectName : javaProject.getRequiredProjectNames()) {
			IProject subProject = workspaceRoot.getProject(projectName);
			for (String s : collectClasspathEntries(subProject))
				if (!classpathEntries.contains(s))
					classpathEntries.add(s);
		}
		return classpathEntries;
	}

	/**
	 * Builds a Java compiler compatible colon-separated string of classapth
	 * entries for a project.
	 *
	 * @param project
	 *            the project to build the classpath string for
	 * @return a Java compiler compatible colon-separated string of classapth
	 *         entries for a project, or <code>null</code> if
	 *         <code>project</code> is not a Java project.
	 * @throws JavaModelException
	 *             if the classpath string could not be determined.
	 */
	public static String buildClasspathString(IProject project) throws JavaModelException {
		List<String> classpathEntries = collectClasspathEntries(project);
		if (classpathEntries == null)
			return null;
		String classpathString = "";
		for (int i = 0; i < classpathEntries.size(); i++)
			classpathString += (i == 0 ? "" : File.pathSeparator) + classpathEntries.get(i);

		return classpathString;
	}

	/**
	 * Returns the name of the package a java file is in.
	 *
	 * @param srcFile
	 *            the java file to get the package for.
	 * @return the name of the package a java file is in, or an empty string if
	 *         the java file is in the default package.
	 * @throws IOException
	 *             if the file is not a java file or the package could otherwise
	 *             not be determined.
	 */
	public static String getPackage(IFile srcFile) throws IOException {
		IJavaElement javaFile = JavaCore.create(srcFile);
		if (javaFile == null)
			throw new IOException("File is not a java file");
		IJavaElement fragment = javaFile.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
		if (fragment == null)
			throw new IOException("Package could not be determinded.");
		IPackageFragment pkg = (IPackageFragment) fragment;
		if (pkg.isDefaultPackage())
			return "";
		return pkg.getElementName();
	}

}
