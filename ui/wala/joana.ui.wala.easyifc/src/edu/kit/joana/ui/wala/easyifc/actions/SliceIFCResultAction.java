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

import com.ibm.wala.util.CancelException;

import edu.kit.joana.ui.wala.easyifc.Activator;
import edu.kit.joana.ui.wala.easyifc.model.CheckInformationFlow;
import edu.kit.joana.ui.wala.easyifc.model.FileSourcePositions;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.FlowStmtResult;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.FlowStmtResultPart;
import edu.kit.joana.ui.wala.easyifc.model.ProgramSourcePositions;
import edu.kit.joana.ui.wala.easyifc.util.CheckFlowMarkerAndImageManager;
import edu.kit.joana.ui.wala.easyifc.util.ProgressMonitorDelegate;
import edu.kit.joana.ui.wala.easyifc.util.ProjectUtil;
import edu.kit.joana.ui.wala.easyifc.views.IFCTreeContentProvider.StmtPartNode;
import edu.kit.joana.ui.wala.easyifc.views.IFCTreeContentProvider.TreeNode;
import edu.kit.joana.ui.wala.easyifc.views.EasyIFCView;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;

public class SliceIFCResultAction extends Action {

	private final EasyIFCView view;

	public SliceIFCResultAction(final EasyIFCView view) {
		this.view = view;
		this.setText("Compute program slice");
		this.setDescription("Compute the program slice under the selected alias configuration.");
		this.setId("joana.ui.wala.easyifc.sliceIFCResultAction");
		this.setImageDescriptor(Activator.getImageDescriptor("icons/run_slice.png"));
	}

	public void run() {
		final TreeNode tn = view.getTree().getSelectedNode();
		if (tn instanceof StmtPartNode) {
			final SliceFlowRunnable sfr = new SliceFlowRunnable((StmtPartNode) tn);
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

	private static class SliceFlowRunnable implements IRunnableWithProgress {

		private final StmtPartNode spn;

		private SliceFlowRunnable(final StmtPartNode spn) {
			this.spn = spn;
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			final FlowStmtResult fsr = spn.getStmtInfo().getResult();
			final FlowStmtResultPart res = spn.getResult();
			try {
				final ProgramSourcePositions pos = CheckInformationFlow.sliceIFCStmt(fsr.getStmt(), res,
						spn.getTmpDir(), ProgressMonitorDelegate.createProgressMonitorDelegate(monitor));

				final IWorkspaceRunnable runnable = new IWorkspaceRunnable() {

					@Override
					public void run(final IProgressMonitor progress) throws CoreException {
						CheckFlowMarkerAndImageManager.getInstance().clearAllSliceMarkers();
						final IJavaProject jp = spn.getProject();
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
								CheckFlowMarkerAndImageManager.getInstance().createSliceMarkers(file, f);
							}
						}
					}
				};

				spn.getProject().getResource().getWorkspace().run(runnable, null, IWorkspace.AVOID_UPDATE, monitor);
			} catch (IOException e) {
				throw new InvocationTargetException(e, e.getMessage());
			} catch (CancelException e) {
				throw new InterruptedException(e.getMessage());
			} catch (FlowAstException e) {
				throw new InvocationTargetException(e, e.getMessage());
			} catch (CoreException e) {
				throw new InvocationTargetException(e, e.getMessage());
			}
		}

	}

}
