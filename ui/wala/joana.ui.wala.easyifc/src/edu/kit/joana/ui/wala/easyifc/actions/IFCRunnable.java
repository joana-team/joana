/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.ui.wala.easyifc.actions.IFCAction.ProjectConf;
import edu.kit.joana.ui.wala.easyifc.model.CheckInformationFlow;
import edu.kit.joana.ui.wala.easyifc.model.CheckInformationFlow.CheckIFCConfig;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.SLeak;
import edu.kit.joana.ui.wala.easyifc.model.IFCResultFilter.LeakType;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer;
import edu.kit.joana.ui.wala.easyifc.model.IFCResultFilter;
import edu.kit.joana.ui.wala.easyifc.util.ProgressMonitorDelegate;
import edu.kit.joana.ui.wala.easyifc.util.EntryPointSearch.EntryPointConfiguration;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class IFCRunnable implements IRunnableWithProgress {

	private final EntryPointConfiguration entryPoint;
	private final ProjectConf conf;
	private final IFCCheckResultConsumer resultConsumer;

	private final IFCResultFilter filter = null;
	
	public IFCRunnable(final ProjectConf conf, final IFCCheckResultConsumer resultConsumer, final EntryPointConfiguration entryPoint) {
		this.conf = conf;
		this.resultConsumer = resultConsumer;
		this.entryPoint = entryPoint;
	}

	@Override
	public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		monitor.beginTask("Running information flow control analysis...", IProgressMonitor.UNKNOWN);
		
		final ProgressMonitorDelegate walaProgress = ProgressMonitorDelegate.createProgressMonitorDelegate(monitor);
		try {
			final String thirdPartyJar;
			if (conf.getJarDirs().size() > 0) {
				thirdPartyJar = conf.getJarDirs().get(0);
			} else {
				thirdPartyJar = null;
			}
			
			final CheckIFCConfig cfc = new CheckIFCConfig(conf.getBinDir(), conf.getSrcDirs().get(0),
					conf.getLibLocation(), thirdPartyJar, conf.getLogOut(), resultConsumer, filter,
					walaProgress, conf.getSelectedIFCType());
			final CheckInformationFlow cfl = new CheckInformationFlow(cfc);
			final IProject project = conf.getProject().getProject();
			monitor.subTask("Refreshing project.");
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			monitor.subTask("Building project.");
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
			monitor.subTask("Checking information flow.");
			cfl.runCheckIFC(entryPoint, walaProgress);
		} catch (ClassHierarchyException e) {
			throw new InvocationTargetException(e, e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new InvocationTargetException(e, e.getMessage());
		} catch (IOException e) {
			throw new InvocationTargetException(e, e.getMessage());
		} catch (CancelException e) {
			throw new InterruptedException(e.getMessage());
		} catch (UnsoundGraphException e) {
			throw new InvocationTargetException(e, e.getMessage());
		} catch (CoreException e) {
			throw new InvocationTargetException(e, e.getMessage());
		}
		
		monitor.done();
	}

}
