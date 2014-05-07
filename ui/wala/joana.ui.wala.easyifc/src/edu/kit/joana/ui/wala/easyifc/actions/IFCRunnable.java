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
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer;
import edu.kit.joana.ui.wala.easyifc.util.ProgressMonitorDelegate;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class IFCRunnable implements IRunnableWithProgress {

	private final ProjectConf conf;
	private final IFCCheckResultConsumer resultConsumer;

	public IFCRunnable(final ProjectConf conf, final IFCCheckResultConsumer resultConsumer) {
		this.conf = conf;
		this.resultConsumer = resultConsumer;
	}

	@Override
	public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		final ProgressMonitorDelegate walaProgress = ProgressMonitorDelegate.createProgressMonitorDelegate(monitor);
		try {
			final CheckIFCConfig cfc = new CheckIFCConfig(conf.getBinDir(), conf.getSrcDirs().get(0), conf.getTempDir(),
					conf.getLibLocation(), conf.getLogOut(), resultConsumer, walaProgress);
			final CheckInformationFlow cfl = new CheckInformationFlow(cfc);
			final IProject project = conf.getProject().getProject();
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
			cfl.runCheckIFC();
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
	}

}
