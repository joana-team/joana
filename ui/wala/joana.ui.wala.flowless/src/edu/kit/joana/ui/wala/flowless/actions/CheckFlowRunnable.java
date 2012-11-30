/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.flowless.actions;

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

import edu.kit.joana.ui.wala.flowless.actions.CheckFlowAction.ProjectConf;
import edu.kit.joana.ui.wala.flowless.util.ProgressMonitorDelegate;
import edu.kit.joana.wala.dictionary.accesspath.CheckFlowLessWithAlias;
import edu.kit.joana.wala.dictionary.accesspath.CheckFlowLessWithAlias.CheckFlowConfig;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class CheckFlowRunnable implements IRunnableWithProgress {

	private final ProjectConf conf;
	private final FlowCheckResultConsumer resultConsumer;

	public CheckFlowRunnable(final ProjectConf conf, final FlowCheckResultConsumer resultConsumer) {
		this.conf = conf;
		this.resultConsumer = resultConsumer;
	}

	@Override
	public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		final ProgressMonitorDelegate walaProgress = ProgressMonitorDelegate.createProgressMonitorDelegate(monitor);
		try {
			final CheckFlowConfig cfc = new CheckFlowConfig(conf.getBinDir(), conf.getSrcDirs().get(0), conf.getTempDir(),
					conf.getLibLocation(), conf.getLogOut(), resultConsumer, walaProgress);
			final CheckFlowLessWithAlias cfl = new CheckFlowLessWithAlias(cfc);
			final IProject project = conf.getProject().getProject();
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
			cfl.runCheckFlowLess();
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
