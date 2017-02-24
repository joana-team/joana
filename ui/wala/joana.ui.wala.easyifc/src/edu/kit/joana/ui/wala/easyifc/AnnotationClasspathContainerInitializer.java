/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.Bundle;

public class AnnotationClasspathContainerInitializer extends ClasspathContainerInitializer {
	private static final String JOANA_ANNOTATIONS_LIBRARY_CONTAINER_ID = "joana.ui.wala.easyifc.JOANA_ANNOTATIONS";

	@Override
	public void initialize(final IPath containerPath, final IJavaProject project) throws CoreException {
		final Bundle bundle = Platform.getBundle("joana.ui.annotations");
		final URL binURL = bundle.getEntry("bin/");
		final URL srcURL = bundle.getEntry("src/");
		try {
			final URL binPath = FileLocator.toFileURL(binURL);
			final URL srcPath = FileLocator.toFileURL(srcURL);
			//
			final IClasspathContainer[] containers = new IClasspathContainer[] {
				new IClasspathContainer() {
					final IPath path = new Path(JOANA_ANNOTATIONS_LIBRARY_CONTAINER_ID);
					@Override
					public IPath getPath() {
						return path;
					}

					@Override
					public int getKind() {
						return K_APPLICATION;
					}

					@Override
					public String getDescription() {
						return "JOANA Source Annotations";
					}

					@Override
					public IClasspathEntry[] getClasspathEntries() {
						return new IClasspathEntry[] {
							JavaCore.newLibraryEntry(
								new Path(binPath.getFile()),
								new Path(srcPath.getFile()),
								new Path(srcPath.getFile())
							),
						};
					}
				}
			};
			JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, containers, null);
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
