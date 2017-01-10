/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
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

import edu.kit.joana.api.SPos;
import edu.kit.joana.api.annotations.AnnotationTypeBasedNodeCollector;
import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ui.wala.easyifc.Activator;
import edu.kit.joana.ui.wala.easyifc.model.FileSourcePositions;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.IFCResult;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.SLeak;
import edu.kit.joana.ui.wala.easyifc.model.ProgramSourcePositions;
import edu.kit.joana.ui.wala.easyifc.util.EasyIFCMarkerAndImageManager;
import edu.kit.joana.ui.wala.easyifc.util.ProjectUtil;
import edu.kit.joana.ui.wala.easyifc.views.EasyIFCView;
import edu.kit.joana.ui.wala.easyifc.views.IFCTreeContentProvider.AnnotationNode;
import edu.kit.joana.ui.wala.easyifc.views.IFCTreeContentProvider.IFCInfoNode;
import edu.kit.joana.ui.wala.easyifc.views.IFCTreeContentProvider.LeakInfoNode;
import edu.kit.joana.ui.wala.easyifc.views.IFCTreeContentProvider.TreeNode;

public class HighlightAnnotationNodesAction extends Action {

	private final EasyIFCView view;

	public HighlightAnnotationNodesAction(final EasyIFCView view) {
		this.view = view;
		this.setText("Highlight corresponding code");
		this.setDescription("Highlight code that corresponds to nodes covered by this annotation.");
		this.setId("joana.ui.wala.easyifc.highlightAnnotationNodesAction");
		this.setImageDescriptor(Activator.getImageDescriptor("icons/highlight_annotation_nodes.png"));
	}

	public void run() {
		final TreeNode<?,?,?> tn = view.getTree().getSelectedNode();
		if (tn instanceof AnnotationNode) {
			final FindNodesRunnable sfr = new FindNodesRunnable((AnnotationNode) tn);
					
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

	private static class FindNodesRunnable implements IRunnableWithProgress {

		private final AnnotationNode annotationNode;

		private FindNodesRunnable(final AnnotationNode annotationNode) {
			this.annotationNode = annotationNode;
		}

		private static ProgramSourcePositions buildSourcePositions(final IFCAnnotation annotation, final AnnotationTypeBasedNodeCollector collector) {
			final ProgramSourcePositions psp = new ProgramSourcePositions();
			
			Collection<SDGNode> nodes = collector.collectNodes(annotation.getProgramPart(), annotation.getType()); 
			for (SDGNode n : nodes) {
				psp.addSourcePosition(n.getSource(), n.getSr(), n.getEr(), n.getSc(), n.getEc());
			}
			// TODO: use logging?!?!
			System.out.println("SDG nodes for " + annotation + ":\t" + nodes);
			return psp;
		}
		
		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			
			
			
			final IFCAnnotation annotation = annotationNode.getAnnotation();
			final ProgramSourcePositions pos = buildSourcePositions(annotation, annotationNode.getCollector());
			final IJavaProject jp = annotationNode.getProject();
			
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
								EasyIFCMarkerAndImageManager.getInstance().createAnnotationMarkers(file, f);
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
