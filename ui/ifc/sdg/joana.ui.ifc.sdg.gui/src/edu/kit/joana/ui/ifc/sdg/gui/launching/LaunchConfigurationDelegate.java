/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.launching;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.IDE;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;
import edu.kit.joana.ui.ifc.sdg.gui.ifcalgorithms.IFCJob;
import edu.kit.joana.ui.ifc.sdg.gui.ifcalgorithms.IFCJobFactory;
import edu.kit.joana.ui.ifc.sdg.gui.sdgworks.SDGFactory;

public class LaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate implements ILaunchConfigurationDelegate {

	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
	throws CoreException {
		try {
			ConfigReader cr = new ConfigReader(configuration);
			IProject p = cr.getIProject();
			SDGFactory sf = NJSecPlugin.singleton().getSDGFactory();

			// auf Annotationen ueberpruefen
			boolean annotations = false;
			IMarker[] markers = NJSecPlugin.singleton().getMarkerFactory().findNJSecMarkers(p, null);
			if (markers.length > 0) {
				annotations = true;
			}

			if (!annotations) {
				monitor.done();
				return;
			}

			SDG sdg = null;

			// load and annotate the SDG
			try {
				// annotates the SDG implicitly
				sdg = sf.loadSDG(cr);

			} catch (IOException e) {
				monitor.isCanceled();
				IStatus status= new Status(IStatus.ERROR, NJSecPlugin.singleton().getSymbolicName(), 0, "Couldn't create or read SDG File", e);
				throw new CoreException(status);
			}

			// schedule job for IFC computation
			IResource resource = cr.getMainClass().getCorrespondingResource();
			if (resource instanceof IFile) {
//				GUITools.showResource(resource);
				IMarker marker;
				marker = resource.createMarker("");
				IWorkbenchPage page = NJSecPlugin.singleton().getActivePage();
				IDE.openEditor(page, marker);
				marker.delete();
			}

			IFCJob job = IFCJobFactory.createIFCJob(cr, monitor, sdg);
			job.schedule();

			if (job.getCoreException() != null) throw job.getCoreException();

			monitor.done();

		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
}
