/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.builder;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import edu.kit.joana.ui.ifc.sdg.compiler.util.Activator;

/**
 * A eclipse runtime job performing a background build of a
 * <code>IProject</code>.
 *
 */
public class BuildJob extends Job {

	private final IProject project;
	private boolean done;
	private boolean cancelled = false;
	private boolean started = false;

	@Override
	protected void canceling() {
		cancelled = true;
	}

	/**
	 * Constructor
	 *
	 * @param project
	 *            the project to be built.
	 */
	public BuildJob(IProject project) {
		super("Update Joana build cache for project '" + project.getName() + "'");
		this.project = project;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			started = true;
			boolean succeeded = JoanaCompiler.build(new FullProjectBuildInput(project), monitor, this);
			done = true;
			if (succeeded)
				return new Status(IStatus.OK, Activator.PLUGIN_ID, IStatus.OK, "Joana build cache update succeeded", null);
			else
				return new Status(IStatus.WARNING, Activator.PLUGIN_ID, IStatus.OK, "Joana build cache update completed with errors", null);
		} catch (CoreException e) {
			e.printStackTrace();
			done = true;
			return e.getStatus();
		}
	}

	/**
	 * Indicates if the job has started
	 *
	 * @return <code>true</code> if the job has already started, else
	 *         <code>false</code>
	 */
	public boolean isStarted() {
		return started;
	}

	/**
	 * Indicates of the job has finished
	 *
	 * @return <code>true</code> if the job has finished. else
	 *         <code>false</code>.
	 */
	public boolean isDone() {
		return done;
	}

	/**
	 * Indicates if the job has been cancelled.
	 *
	 * @return <code>true</code> if the job has been cancelled, else
	 *         <code>false</code>.
	 */
	public boolean cancelled() {
		return cancelled;
	}

}
