/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import edu.kit.joana.ui.ifc.sdg.compiler.builder.IJoanaBuildProblemMarker;
import edu.kit.joana.ui.ifc.sdg.compiler.util.JoanaNatureUtil;

/**
 *
 * External functionality of the edu.kit.joana.ui.ifc.sdg.compiler plugin. Provides access to
 * IFC compiler output.
 *
 * @author Alexander Stockinger
 *
 */
public final class IfcCompiler {

	private IfcCompiler() {}

	/**
	 * Returns a <code>File</code> object pointing to the compilation result
	 * of the IFC compiler for the provided <code>IFile</code>.
	 *
	 * @param file
	 *            the <code>IFile</code> in the workspace to find the
	 *            compilation result for.
	 *
	 * @return a <code>File</code> object pointing to the compilation result
	 *         of the IFC compiler for the provided <code>IFile</code>.
	 *
	 * @throws CompilerErrorException
	 *             if the compilation result could not be provided because the
	 *             file could not be compiled.
	 * @throws NotBuildingException
	 *             if the provided <code>IFile</code> is not part of any IFC
	 *             build process.
	 * @throws IOException
	 *             if a I/O error occured while determining the location of the
	 *             compilation result.
	 * @throws CoreException
	 *             if an error occured in the Eclipse core library.
	 */
	public static File getIfcClassFile(IFile file) throws CompilerErrorException, NotBuildingException, IOException, CoreException {
		assert file != null;
		if (!buildsFile(file))
			throw new NotBuildingException();
		if (!buildSucceeded(file))
			throw new CompilerErrorException(getCompilerError(file));
		return JoanaNatureUtil.getDestinationFile(file);
	}

	/**
	 * Returns a description of the compilation problem.
	 *
	 * @param file
	 *            the file to get the compilation problem for.
	 * @return a description of the compilation problem, or <code>null</code>
	 *         if the compilation was successful.
	 * @throws NotBuildingException
	 *             if the <code>file</code> is not path of an IFC build
	 *             process.
	 */
	public static String getCompilerError(IFile file) throws NotBuildingException {
		assert file != null;
		if (!buildsFile(file))
			throw new NotBuildingException();
		try {
			IMarker markers[] = file.findMarkers(IJoanaBuildProblemMarker.ID, true, IResource.DEPTH_INFINITE);
			if (markers.length != 0)
				return markers[0].getAttribute(IJoanaBuildProblemMarker.COMPILER_OUTPUT).toString();
			File destinationFile = JoanaNatureUtil.getDestinationFile(file);
			if (!destinationFile.exists())
				return "Destination file not found: " + destinationFile.getCanonicalPath();
			if (!destinationFile.isFile())
				return "Destination path does not point to a file: " + destinationFile.getCanonicalPath();
			return null;
		} catch (IOException e) {
			return e.getMessage() == null ? "Unknown I/O error" : e.getMessage();
		} catch (CoreException e) {
			return "Could not determine compilation state: " + e.getMessage();
		}
	}

	/**
	 * Indicates of a <code>IFile</code> is part of a IFC build process.
	 *
	 * @param file
	 *            the <code>IFile</code> to check.
	 *
	 * @return <code>true</code> if the provided <code>IFile</code> is part
	 *         of an IFC build process and a compilation result can be expected,
	 *         else <code>false</code>
	 */
	public static boolean buildsFile(IFile file) {
		assert file != null;
		return JoanaNatureUtil.isBuildResource(file);
	}

	/**
	 * Indicates if the IFC build process for a provided <code>IFile</code>
	 * succeeded.
	 *
	 * @param file
	 *            the <code>IFile</code> to check.
	 * @return <code>true</code> if the provided <code>IFile</code> is part
	 *         of an IFC build process, the compilation succeeded and a handle
	 *         to the compilation result can be retrieved via
	 *         <code>getIfcClassFile()</code>. Else <code>false</code>.
	 * @throws NotBuildingException
	 *             if the provided <code>IFile</code> is not part of an IFC
	 *             build process.
	 */
	public static boolean buildSucceeded(IFile file) throws NotBuildingException {
		assert file != null;
		if (!buildsFile(file))
			throw new NotBuildingException();
		return getCompilerError(file) == null;
	}
}
