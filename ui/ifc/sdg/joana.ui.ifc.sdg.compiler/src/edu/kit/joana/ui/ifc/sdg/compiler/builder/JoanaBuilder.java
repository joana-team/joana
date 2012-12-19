/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.builder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import edu.kit.joana.ui.ifc.sdg.compiler.util.Activator;
import edu.kit.joana.ui.ifc.sdg.compiler.util.JoanaNatureUtil;
import edu.kit.joana.ui.ifc.sdg.compiler.util.PluginUtil;

/**
 * A eclipse resources <code>IncrementalProjectBuilder</code> for integrating
 * the JOANA compiler into the eclipse build process.
 *
 */
public class JoanaBuilder extends IncrementalProjectBuilder {
	/**
	 * The qualified ID of the builder.
	 */
	public static final String ID = Activator.PLUGIN_ID + ".joanaBuilder";

	private static Map<IProject, BuildJob> runningJobs = new HashMap<IProject, BuildJob>();

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		super.clean(monitor);
		File folder;
		try {
			folder = JoanaNatureUtil.getProjectBuildFolder(getProject());
		} catch (IOException e) {
			throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, IStatus.OK, "Could not get project build folder", e));
		}
		if (!folder.exists() || !folder.isDirectory())
			return;
		File[] dirContent = folder.listFiles();
		monitor.beginTask("Cleanup Joana build cache", dirContent.length);
		for (File res : dirContent) {
			if (res.isFile() || res.isDirectory())
				res.delete();
			monitor.worked(1);
		}
		monitor.done();
	}

	private IWorkbench workbench;

	/**
	 * Constructor
	 */
	public JoanaBuilder() {
		workbench = PlatformUI.getWorkbench();
	}

	@SuppressWarnings("unchecked")
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) {
		try {
			IProject project = getProject();
			if (project != null && project.isAccessible()) {
				switch (kind) {
				case INCREMENTAL_BUILD:
				case AUTO_BUILD:
				case FULL_BUILD:
					scheduleBuild(project, true);
					break;
				}
				monitor.done();
			}
		} catch (Exception x) {
			// TODO: useful message here
			ErrorDialog.openError(workbench.getActiveWorkbenchWindow().getShell(), "Dialog title", "BuildError", Status.CANCEL_STATUS);
		}
		return new IProject[0];
	}

	@Override
	protected void startupOnInitialize() {
		build(IncrementalProjectBuilder.FULL_BUILD, null, PluginUtil.getDefaultProgressMonitor());
	}

	/**
	 * Schedules a new background build job.
	 *
	 * @param project
	 *            the project to be built.
	 * @param abortRunning
	 *            indicates if a running background build of the specified
	 *            project shall be cancelled.
	 */
	public static void scheduleBuild(final IProject project, boolean abortRunning) {
		synchronized (runningJobs) {
			BuildJob runningJob = runningJobs.get(project);
			if (runningJob != null) {
				if (!runningJob.isStarted())
					return;
				if (abortRunning && !runningJob.isDone()) {
					runningJob.cancel();
				}
			}
			runningJob = new BuildJob(project);
			runningJob.schedule();
			runningJobs.put(project, runningJob);
		}
	}
}
