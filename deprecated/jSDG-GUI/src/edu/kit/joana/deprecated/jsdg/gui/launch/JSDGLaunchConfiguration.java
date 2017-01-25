/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.gui.launch;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;

import com.ibm.wala.ide.util.ProgressMonitorDelegate;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

import edu.kit.joana.deprecated.jsdg.Activator;
import edu.kit.joana.deprecated.jsdg.SDGFactory;
import edu.kit.joana.deprecated.jsdg.sdg.interference.CSDGPreprocessor;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.ifc.sdg.graph.SDG;

public class JSDGLaunchConfiguration extends AbstractJavaLaunchConfigurationDelegate implements ILaunchConfigurationDelegate {

	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		SDGFactory sdgFactory = Activator.getDefault().getFactory();
		LaunchConfigReader confReader = new LaunchConfigReader(configuration);
		SDGFactory.Config conf = confReader.getSDGConfig();
		try {
			ProgressMonitorDelegate delegate = ProgressMonitorDelegate.createProgressMonitorDelegate(monitor);
			SDG sdg = sdgFactory.getJoanaSDG(conf, delegate);
			if (conf.outputDir != null) {
				String fileName = conf.outputDir + File.separator + conf.outputSDGfile;

				monitor.beginTask("Saving SDG to " + fileName, -1);
                CSDGPreprocessor.createAndSaveCSDG(sdg, fileName);
                monitor.done();
			}
		} catch (InvalidClassFileException e) {
			edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().showError(e, "A " + e.getClass().getSimpleName() + " has occured");
		} catch (ClassHierarchyException e) {
			edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().showError(e, "A " + e.getClass().getSimpleName() + " has occured");
		} catch (CancelException e) {
			edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().showError(e, "A " + e.getClass().getSimpleName() + " has occured");
		} catch (IllegalArgumentException e) {
			edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().showError(e, "A " + e.getClass().getSimpleName() + " has occured");
		} catch (PDGFormatException e) {
			edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().showError(e, "A " + e.getClass().getSimpleName() + " has occured");
		} catch (IOException e) {
			edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().showError(e, "A " + e.getClass().getSimpleName() + " has occured");
		} catch (WalaException e) {
			edu.kit.joana.deprecated.jsdg.gui.Activator.getDefault().showError(e, "A " + e.getClass().getSimpleName() + " has occured");
		}
	}

}
