/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;

import edu.kit.joana.ui.wala.easyifc.Activator;
import edu.kit.joana.ui.wala.easyifc.model.FileSourcePositions;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.IFCResult;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.SLeak;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.SPos;
import edu.kit.joana.ui.wala.easyifc.model.ProgramSourcePositions;
import edu.kit.joana.ui.wala.easyifc.util.EasyIFCMarkerAndImageManager;
import edu.kit.joana.ui.wala.easyifc.util.ProjectUtil;
import edu.kit.joana.ui.wala.easyifc.views.EasyIFCView;
import edu.kit.joana.ui.wala.easyifc.views.IFCTreeContentProvider.IFCInfoNode;
import edu.kit.joana.ui.wala.easyifc.views.IFCTreeContentProvider.LeakInfoNode;
import edu.kit.joana.ui.wala.easyifc.views.IFCTreeContentProvider.TreeNode;

public class HighlightIFCResultAction extends Action {

	private final EasyIFCView view;

	public HighlightIFCResultAction(final EasyIFCView view) {
		this.view = view;
		this.setText("Highlight critical program parts");
		this.setDescription("Compute the program chop between source and sink.");
		this.setId("joana.ui.wala.easyifc.highlightIFCResultAction");
		this.setImageDescriptor(Activator.getImageDescriptor("icons/run_slice.png"));
	}

	public void run() {
		final TreeNode tn = view.getTree().getSelectedNode();
		if (tn instanceof LeakInfoNode || tn instanceof IFCInfoNode) {
			final ChopFlowRunnable sfr;
			if (tn instanceof LeakInfoNode) {
				sfr = new ChopFlowRunnable((LeakInfoNode) tn);
			} else {
				sfr = new ChopFlowRunnable((IFCInfoNode) tn);
			}
					
			try {
				PlatformUI.getWorkbench().getProgressService().run(true, true, sfr);
			} catch (InvocationTargetException e) {
				view.showMessage(e.getMessage());
				e.printStackTrace();
			} catch (InterruptedException e) {
				view.showMessage(e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private static class ChopFlowRunnable implements IRunnableWithProgress {

		private final LeakInfoNode leaknfo;
		private final IFCInfoNode ifcnfo;

		private ChopFlowRunnable(final LeakInfoNode leaknfo) {
			this.leaknfo = leaknfo;
			this.ifcnfo = null;
		}

		private ChopFlowRunnable(final IFCInfoNode ifcnfo) {
			this.leaknfo = null;
			this.ifcnfo = ifcnfo;
		}

		private static ProgramSourcePositions buildSourcePositions(final SLeak leak) {
			final ProgramSourcePositions psp = new ProgramSourcePositions();
			
			for (final SPos pos : leak.getChop()) {
				psp.addSourcePosition(pos.sourceFile, pos.startLine, pos.endLine, pos.startChar, pos.endChar);
			}
			
			return psp;
		}
		
		private static ProgramSourcePositions buildSourcePositions(final IFCResult result) {
			final ProgramSourcePositions psp = new ProgramSourcePositions();

			for (final SLeak leak : result.getLeaks()) {
				for (final SPos pos : leak.getChop()) {
					psp.addSourcePosition(pos.sourceFile, pos.startLine, pos.endLine, pos.startChar, pos.endChar);
				}
			}

			return psp;
		}
		
		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			final ProgramSourcePositions pos;
			final IJavaProject jp;
			
			if (leaknfo != null) {
				final SLeak leak = leaknfo.getLeak();
				pos = buildSourcePositions(leak);
				jp = leaknfo.getProject();
			} else if (ifcnfo != null) {
				final IFCResult result = ifcnfo.getResult();
				pos = buildSourcePositions(result);
				jp = ifcnfo.getProject();
			} else {
				return;
			}
			
			try {
				final IWorkspaceRunnable runnable = new IWorkspaceRunnable() {

					@Override
					public void run(final IProgressMonitor progress) throws CoreException {
						EasyIFCMarkerAndImageManager.getInstance().clearAllSliceMarkers();
						final List<IPath> srcs = ProjectUtil.findProjectSourcePaths(jp);
						final IProject p = jp.getProject();
						final IWorkspaceRoot root = p.getWorkspace().getRoot();

						for (final FileSourcePositions f : pos.getFileSourcePositions()) {
							IFile file = null;

							for (final IPath path : srcs) {
								final IPath pfile = path.append(f.getFilename());
								file = root.getFile(pfile);
								if (file.exists()) {
									break;
								}
							}

							if (file.exists()) {
								EasyIFCMarkerAndImageManager.getInstance().createChopMarkers(file, f);
							}
						}
					}
				};

				jp.getResource().getWorkspace().run(runnable, null, IWorkspace.AVOID_UPDATE, monitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e, e.getMessage());
			}
		}

	}

}
