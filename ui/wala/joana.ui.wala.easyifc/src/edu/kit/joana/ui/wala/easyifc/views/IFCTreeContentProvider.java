/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.views;

import java.util.ArrayList;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.SourceRefElement;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer;
import edu.kit.joana.ui.wala.easyifc.util.CheckFlowMarkerAndImageManager;
import edu.kit.joana.ui.wala.easyifc.util.MethodSearch;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.flowless.spec.FlowLessBuilder.FlowError;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
@SuppressWarnings("restriction")
public class IFCTreeContentProvider implements ITreeContentProvider, IFCCheckResultConsumer {

	private final RootNode root = new RootNode();
	private final EasyIFCView view;

	public IFCTreeContentProvider(final EasyIFCView view) {
		this.view = view;
	}

	private static class TreeSorter extends ViewerSorter {
	    public int compare(Viewer viewer, Object e1, Object e2) {
	    	if (e1 instanceof TreeNode && e2 instanceof TreeNode && e1.getClass() == e2.getClass()) {
	    		if (e1 instanceof MethodInfoNode) {
	    			final MethodInfoNode m1 = (MethodInfoNode) e1;
	    			final MethodInfoNode m2 = (MethodInfoNode) e2;

	    			final String clsName1 = m1.result.getInfo().getClassInfo().getBytecodeName();
	    			final String clsName2 = m2.result.getInfo().getClassInfo().getBytecodeName();

	    			if (clsName1.equals(clsName2)) {
	    				return m1.result.getInfo().getLine() - m2.result.getInfo().getLine();
	    			} else {
	    				return clsName1.compareTo(clsName2);
	    			}
	    		} else if (e1 instanceof StmtInfoNode) {
	    			final StmtInfoNode s1 = (StmtInfoNode) e1;
	    			final StmtInfoNode s2 = (StmtInfoNode) e2;

	    			return s1.result.getStmt().getLineNr() - s2.result.getStmt().getLineNr();
	    		} else if (e1 instanceof StmtPartNode) {
	    			final StmtPartNode p1 = (StmtPartNode) e1;
	    			final StmtPartNode p2 = (StmtPartNode) e2;

	    			return getNumber(p1) - getNumber(p2);
	    		} else {
		    		return super.compare(viewer, e1, e2);
	    		}
	    	} else {
	    		return super.compare(viewer, e1, e2);
	    	}
	    }

	    private int getNumber(final StmtPartNode n) {
	    	if (!n.result.isInferred()) {
	    		if (n.result.getExceptionConfig() != ExceptionAnalysis.IGNORE_ALL) {
	    			if (n.result.isSatisfied()) {
	    				return 1;
	    			} else {
	    				return 2;
	    			}
	    		} else {
	    			if (n.result.isSatisfied()) {
	    				return 3;
	    			} else {
	    				return 4;
	    			}
	    		}
	    	} else {
	    		if (n.result.getExceptionConfig() != ExceptionAnalysis.IGNORE_ALL) {
	    			if (n.result.isSatisfied()) {
	    				return 5;
	    			} else {
	    				return 6;
	    			}
	    		} else {
	    			if (n.result.isSatisfied()) {
	    				return 7;
	    			} else {
	    				return 8;
	    			}
	    		}
	    	}
	    }

	    public int category(Object element) {
	        return 0;
	    }

	}

	public static ViewerSorter makeSorter() {
		return new TreeSorter();
	}

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		if (oldInput != newInput) {
			if (newInput instanceof MethodResult) {
				consume((MethodResult) newInput);
			} else {
				root.clear();
				final IJavaProject jp = view.getCurrentProject();
				CheckFlowMarkerAndImageManager.getInstance().clearAll(jp != null ? jp.getProject() : null);
			}

//			System.out.println("Content changed: " + (oldInput == null ? "null" : oldInput.hashCode())
//					+ " -> " + (newInput == null ? "null" : newInput.hashCode()));
		}
	}

	public void dispose() {
		root.clear();
	}

	public Object[] getElements(Object parent) {
		return root.getChildren();
	}

	@Override
	public Object[] getChildren(Object parent) {
		if (parent instanceof TreeNode) {
			return ((TreeNode) parent).getChildren();
		} else if (parent == null) {
			return root.getChildren();
		}

		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof TreeNode) {
			final TreeNode tn = (TreeNode) element;
			return tn.parent;
		}

		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof TreeNode) {
			return ((TreeNode) element).hasChildren();
		} else if (element == null) {
			return root.hasChildren();
		}

		return false;
	}

	@Override
	public void consume(final MethodResult res) {
		final IJavaProject jp = view.getCurrentProject();
		final MethodInfoNode cur = new MethodInfoNode(root, res);
		cur.searchMatchingJavaElement(jp);

		if (!res.hasErrors()) {
			for (final FlowStmtResult fl : res.getStmtResults()) {
				final StmtInfoNode child = new StmtInfoNode(cur, fl);
				child.searchMatchingJavaElement(jp);

				for (final FlowStmtResultPart flpart : fl.getParts()) {
					final TreeNode part = new StmtPartNode(child, flpart);
					part.searchMatchingJavaElement(jp);
				}
			}
		} else {
			for (final FlowError ferr : res.getErrors()) {
				final FlowErrorNode fn = new FlowErrorNode(cur, ferr);
				fn.searchMatchingJavaElement(jp);
			}
		}
	}

	public static abstract class TreeNode {
		private final TreeNode parent;
		private final ArrayList<TreeNode> children;

		private TreeNode(final TreeNode parent) {
			this.parent = parent;
			this.children = new ArrayList<IFCTreeContentProvider.TreeNode>();
			if (parent != null) {
				parent.addChild(this);
			}
		}

		public boolean hasChildren() {
			return !children.isEmpty();
		}

		private void addChild(final TreeNode child) {
			assert child.parent == this;
			this.children.add(child);
		}

		public final TreeNode[] getChildren() {
			return children.toArray(new TreeNode[children.size()]);
		}

		public final TreeNode getParent() {
			return parent;
		}

		public abstract SourceRefElement getSourceRef();
		public abstract IMarker getMarker();
		public abstract IMarker getSideMarker();

		public abstract void searchMatchingJavaElement(final IJavaProject project);

		public abstract String toString();

		public Image getImage() {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}

		public IJavaProject getProject() {
			return (parent != null ? parent.getProject() : null);
		}

		public String getTmpDir() {
			return (parent != null ? parent.getTmpDir() : null);
		}

	}

	public static final class RootNode extends TreeNode {
		private RootNode() {
			super(null);
		}

		public String toString() {
			return "Results of ifc flow checker.";
		}

		public void clear() {
			super.children.clear();
		}

		@Override
		public void searchMatchingJavaElement(final IJavaProject project) {
			// there is none for the root node
		}

		@Override
		public SourceRefElement getSourceRef() {
			return null;
		}

		@Override
		public IMarker getMarker() {
			return null;
		}

		@Override
		public IMarker getSideMarker() {
			return null;
		}

	}

	public static final class MethodInfoNode extends TreeNode {
		private final MethodResult result;
		private IJavaProject project;
		private MethodSearch search = null;
		private IMarker sideMarker = null;

		private MethodInfoNode(final TreeNode parent, final MethodResult result) {
			super(parent);
			this.result = result;
		}

		public String toString() {
			if (result.hasErrors()) {
				return result.getInfo().toString() + " - " + result.getErrors().size() + " errors.";
			} else {
				return result.getInfo().toString();
			}
		}

		@Override
		public void searchMatchingJavaElement(final IJavaProject project) {
			this.project = project;
			search = MethodSearch.searchMethod(project, result);
		}

		@Override
		public SourceRefElement getSourceRef() {
			return (search != null ? search.getMethod() : null);
		}

		@Override
		public IMarker getMarker() {
			return null;
		}

		@Override
		public Image getImage() {
			if (search != null) {
				final SourceMethod m = search.getMethod();
				return CheckFlowMarkerAndImageManager.getInstance().getImage(m);
			}

			return super.getImage();
		}

		@Override
		public IMarker getSideMarker() {
			return sideMarker;
		}

		@Override
		public String getTmpDir() {
			return result.getTmpDir();
		}

		@Override
		public IJavaProject getProject() {
			return project;
		}
	}

	public static final class FlowErrorNode extends TreeNode {

		private final FlowError ferr;
		private IMarker marker = null;
		private IMarker sideMarker = null;

		private FlowErrorNode(final MethodInfoNode parent, final FlowError ferr) {
			super(parent);
			this.ferr = ferr;
		}

		public String toString() {
			String msg = ferr.exc.getMessage();
			if (msg == null || msg.isEmpty()) {
				msg = ferr.exc.toString();
			}

			return msg;
		}

		public FlowError getError() {
			return ferr;
		}

		public MethodInfoNode getMethodInfo() {
			return (MethodInfoNode) getParent();
		}

		@Override
		public void searchMatchingJavaElement(final IJavaProject project) {
			final MethodSearch search = getMethodInfo().search;
			if (search != null) {
				marker = search.findFlowError(ferr);
				sideMarker = search.makeSideMarker(ferr);
			}
		}

		@Override
		public SourceRefElement getSourceRef() {
			return getMethodInfo().getSourceRef();
		}

		@Override
		public IMarker getMarker() {
			return marker;
		}

		@Override
		public IMarker getSideMarker() {
			return sideMarker;
		}

		@Override
		public Image getImage() {
			return CheckFlowMarkerAndImageManager.getInstance().getImage(ferr);
		}

	}

	public static final class StmtInfoNode extends TreeNode {
		private final FlowStmtResult result;
		private IMarker marker = null;
		private IMarker sideMarker = null;

		private StmtInfoNode(final MethodInfoNode parent, final FlowStmtResult result) {
			super(parent);
			this.result = result;
		}

		public String toString() {
			return result.getStmt().toString();
		}

		@Override
		public void searchMatchingJavaElement(final IJavaProject project) {
			final MethodSearch search = getMethodInfo().search;
			if (search != null) {
				marker = search.findIFCStmt(result.getStmt());
				sideMarker = search.makeSideMarker(result);
			}
		}

		public FlowStmtResult getResult() {
			return result;
		}

		public MethodInfoNode getMethodInfo() {
			return (MethodInfoNode) getParent();
		}

		@Override
		public SourceRefElement getSourceRef() {
			return getMethodInfo().getSourceRef();
		}

		@Override
		public IMarker getMarker() {
			return marker;
		}

		@Override
		public IMarker getSideMarker() {
			return sideMarker;
		}

		@Override
		public Image getImage() {
			return CheckFlowMarkerAndImageManager.getInstance().getImage(result);
		}

	}

	public static final class StmtPartNode extends TreeNode {
		private final FlowStmtResultPart result;

		private StmtPartNode(final StmtInfoNode parent, final FlowStmtResultPart result) {
			super(parent);
			this.result = result;
		}

		public String toString() {
			if (result.isSatisfied()) {
				if (result.isInferred()) {
					if (result.getExceptionConfig() == ExceptionAnalysis.IGNORE_ALL) {
						return "Satisfied without the effect of control flow through exceptions and "
								+ "under the inferred alias context: " + result.getDescription();
					} else {
						return "Satisfied under the inferred alias context: " + result.getDescription();
					}
				} else {
					if (result.getExceptionConfig() == ExceptionAnalysis.IGNORE_ALL) {
						return "Satisfied without the effect of control flow through exceptions.";
					} else {
						return "Always satisfied.";
					}
				}
			} else {
				if (!result.isInferred()) {
					if (result.getExceptionConfig() == ExceptionAnalysis.IGNORE_ALL) {
						return "Could not be validated, even without the effect of control flow through exceptions.";
					} else {
						return "Could not be validated in general.";
					}
				} else {
					if (result.getExceptionConfig() == ExceptionAnalysis.IGNORE_ALL) {
						return "Could not infere a valid alias context, even without the effect of control flow through exceptions.";
					} else {
						return "Could not infere a valid alias context.";
					}
				}
			}
		}

		@Override
		public void searchMatchingJavaElement(final IJavaProject project) {
		}

		public FlowStmtResultPart getResult() {
			return result;
		}

		public StmtInfoNode getStmtInfo() {
			return (StmtInfoNode) getParent();
		}

		@Override
		public SourceRefElement getSourceRef() {
			return getStmtInfo().getSourceRef();
		}

		@Override
		public IMarker getMarker() {
			return getStmtInfo().getMarker();
		}

		@Override
		public IMarker getSideMarker() {
			return null;
		}

		@Override
		public Image getImage() {
			return CheckFlowMarkerAndImageManager.getInstance().getImage(result);
		}

	}

	public Object getRoot() {
		return root;
	}

 }
