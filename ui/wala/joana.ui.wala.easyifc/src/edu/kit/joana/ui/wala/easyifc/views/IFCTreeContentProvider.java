/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.views;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.core.SourceRefElement;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.google.common.collect.Multimap;

import edu.kit.joana.api.SPos;
import edu.kit.joana.api.annotations.AnnotationType;
import edu.kit.joana.api.annotations.AnnotationTypeBasedNodeCollector;
import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer;
import edu.kit.joana.ui.wala.easyifc.util.EasyIFCMarkerAndImageManager;
import edu.kit.joana.ui.wala.easyifc.util.EasyIFCMarkerAndImageManager.Marker;
import edu.kit.joana.ui.wala.easyifc.util.EntryPointSearch.EntryPointConfiguration;
import edu.kit.joana.util.Pair;
import edu.kit.joana.ui.wala.easyifc.util.SearchHelper;

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
	    		if (e1 instanceof LeakInfoNode) {
	    			final LeakInfoNode m1 = (LeakInfoNode) e1;
	    			final LeakInfoNode m2 = (LeakInfoNode) e2;

	    			final SLeak l1 = m1.getLeak();
	    			final SLeak l2 = m2.getLeak();
	    			
	    			if (l1.getReason().importance != l2.getReason().importance) {
	    				return l1.getReason().importance - l2.getReason().importance;
	    			}
	    			
	    			return l1.compareTo(l2);
	    		} else {
		    		return super.compare(viewer, e1, e2);
	    		}
	    	} else {
	    		return super.compare(viewer, e1, e2);
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
			if (newInput instanceof IFCResult) {
				consume((IFCResult) newInput);
			} else if (newInput instanceof EntryPointConfiguration){
				inform((EntryPointConfiguration) newInput);
			} else {
				root.clear();
				final IJavaProject jp = view.getCurrentProject();
				EasyIFCMarkerAndImageManager.getInstance().clearAll(jp != null ? jp.getProject() : null);
			}
		}
	}

	public void dispose() {
		root.clear();
	}

	@Override
	public Object[] getElements(Object parent) {
		return root.getChildren().toArray();
	}

	@Override
	public Object[] getChildren(Object parent) {
		if (parent instanceof TreeNode) {
			return ((TreeNode<?,?,?>) parent).getChildren().toArray();
		} else if (parent == null) {
			return root.getChildren().toArray();
		}

		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof TreeNode) {
			final TreeNode<?,?,?> tn = (TreeNode<?,?,?>) element;
			return tn.parent;
		}

		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof TreeNode) {
			return ((TreeNode<?,?,?>) element).hasChildren();
		} else if (element == null) {
			return root.hasChildren();
		}

		return false;
	}

	@Override
	public void consume(final IFCResult res) {
		final IJavaProject jp = view.getCurrentProject();
		final IFCInfoNode cur = new IFCInfoNode(root, res, jp); // implicitly added to root
		final AnnotationsNode annRoot = new AnnotationsNode(cur);
		if (!res.getEntryPointConfiguration().isDefaultParameters()) {
			new IFCConfigurationInfoNode(cur, res.getEntryPointConfiguration());
		}
		cur.searchMatchingJavaElement();
		
		final List<LeakInfoNode> nodes = new LinkedList<LeakInfoNode>();

		for (final SLeak leak : res.getLeaks()) {
			final LeakInfoNode lnfo = new LeakInfoNode(cur, leak);
			nodes.add(lnfo);
			if (leak.hasTrigger()) {
				for (final SPos trigger : leak.getTrigger()) {
					new TriggerInfoNode(lnfo, trigger);
				}
			}
		}
		
		/*
		Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> annotations = res.getAnnotations();
		final Multimap<SDGProgramPart, Pair<Source, String>> sources = annotations.getFirst();
		final Multimap<SDGProgramPart, Pair<Sink,   String>> sinks   = annotations.getSecond();

		for (Map.Entry<SDGProgramPart, Pair<Source, String>> e : sources.entries()) {
			new AnnotationNode<Source>(annRoot, e.getKey().toString(), e.getValue().getFirst());
		}
		for (Map.Entry<SDGProgramPart, Pair<Sink, String>> e : sinks.entries()) {
			new AnnotationNode<Sink>(annRoot, e.getKey().toString(), e.getValue().getFirst());
		}
		*/
		Collection<IFCAnnotation> annotations2 = res.getAnnotations2();
		for (IFCAnnotation a : annotations2) {
			new AnnotationNode(annRoot, a.getProgramPart().toString(), a, cur.search, res.getCollector());
		}

		createSideMarkerByImportance(nodes);
	}

	@Override
	public void inform(EntryPointConfiguration discovered) {
		final IJavaProject jp = view.getCurrentProject();
		new NotRunYetNode(root, discovered, jp); // implicitly added to root
	}

	
	private static void createSideMarkerByImportance(final List<LeakInfoNode> nodes) {
		Collections.sort(nodes, new Comparator<LeakInfoNode>() {
			@Override
			public int compare(final LeakInfoNode o1, final LeakInfoNode o2) {
				return o1.getLeak().getReason().importance - o2.getLeak().getReason().importance;
			}});
		
		for (final LeakInfoNode lnfo : nodes) {
			lnfo.searchMatchingJavaElement();
		}
	}
	
	public static abstract class TreeNode<Self extends TreeNode<Self,C,P>, C extends TreeNode<C,?,Self>, P extends TreeNode<P, Self,?>> {
		private final P parent;
		private final ArrayList<C> children;

		protected TreeNode(final P parent) {
			this.parent = parent;
			this.children = new ArrayList<C>();
			if (parent != null) {
				parent.addChild(self());
			}
		}
		
		abstract Self self();

		public boolean hasChildren() {
			return !children.isEmpty();
		}

		protected void addChild(final C child) {
			assert child.getParent() == this;
			this.children.add(child);
		}

		protected void removeChild(final C child) {
			this.children.remove(child);
		}

		public final List<C> getChildren() {
			return Collections.unmodifiableList(children);
		}

		public final P getParent() {
			return parent;
		}

		public abstract SourceRefElement getSourceRef();
		public abstract IMarker[] getSideMarker();

		public abstract void searchMatchingJavaElement();

		public abstract String toString();

		public Image getImage() {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}

		public IJavaProject getProject() {
			return (parent != null ? parent.getProject() : null);
		}

	}
	
	public abstract static class Some<P extends TreeNode<P, Some<P>,?>> extends TreeNode<Some<P>, Some<Some<P>>, P>{
		private Some() {
			super(null);
		}
	}
	public abstract static class None<C extends TreeNode<C, ?, None<C>>> extends TreeNode<None<C>, C, None<None<C>>>{
		private None() {
			super(null);
		}
	}
	public abstract static class SecondLevelNode extends TreeNode<SecondLevelNode, ThirdLevelNode, IFCInfoNode>{
		private SecondLevelNode(IFCInfoNode parent) {
			super(parent);
		}
	}

	public abstract static class ThirdLevelNode extends TreeNode<ThirdLevelNode, Some<ThirdLevelNode>, SecondLevelNode>{
		private ThirdLevelNode(SecondLevelNode parent) {
			super(parent);
		}
	}

	public static final class RootNode extends TreeNode<RootNode,IFCInfoNode,None<RootNode>> {
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
		public void searchMatchingJavaElement() {
			// there is none for the root node
		}

		@Override
		public SourceRefElement getSourceRef() {
			return null;
		}

		@Override
		public IMarker[] getSideMarker() {
			return null;
		}

		@Override
		RootNode self() {
			return this;
		}

	}

	public static final class IFCConfigurationInfoNode extends SecondLevelNode {
		private final EntryPointConfiguration configuration;

		public IFCConfigurationInfoNode(final IFCInfoNode parent, final EntryPointConfiguration configuration) {
			super(parent);
			this.configuration = configuration;
		}
		
		@Override
		public SourceRefElement getSourceRef() {
			return configuration.getSourceRef();
		}

		@Override
		public IMarker[] getSideMarker() {
			return null;
		}

		@Override
		public void searchMatchingJavaElement() {
		}

		@Override
		public String toString() {
			return configuration.toString();
		}

		@Override
		SecondLevelNode self() {
			return this;
		}

		public Image getImage() {
			return EasyIFCMarkerAndImageManager.getInstance().getImage(configuration);
		}

	}
	
	public static final class AnnotationsNode extends SecondLevelNode {
		public AnnotationsNode(final IFCInfoNode parent) {
			super(parent);
		}
		
		@Override
		public IMarker[] getSideMarker() {
			return null;
		}

		@Override
		public void searchMatchingJavaElement() {
		}

		@Override
		public String toString() {
			return "Source and Sink annotations";
		}

		@Override
		SecondLevelNode self() {
			return this;
		}

		public Image getImage() {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
		}
		@Override
		public SourceRefElement getSourceRef() {
			return null;
		}
	}
	
	public static final class AnnotationNode extends ThirdLevelNode {
		private final String element;
		private final IMarker marker;
		private final IFCAnnotation a;
		private final SearchHelper search;
		private final AnnotationTypeBasedNodeCollector collector;
		
		public AnnotationNode(final AnnotationsNode parent, String element, IFCAnnotation a, SearchHelper search, AnnotationTypeBasedNodeCollector collector) {
			super(parent);
			if (!(AnnotationType.SINK.equals(a.getType()) || (AnnotationType.SOURCE.equals(a.getType())))) {
				parent.removeChild(this);
				throw new IllegalArgumentException(a.toString());
			}
			this.element = element;
			this.a = a;
			this.search = search;
			this.collector = collector;
			
			if (a.getCause().getSourcePosition() == null) {
				this.marker = null;
			} else {
				this.marker = search.createSideMarker(
					a.getCause().getSourcePosition(),
					"Annotation",
					AnnotationType.SOURCE.equals(a.getType()) ? Marker.SOURCE : Marker.SINK
				);
			}
		}
		
		@Override
		public IMarker[] getSideMarker() {
			return new IMarker[] { marker };
		}

		@Override
		public void searchMatchingJavaElement() {
		}

		@Override
		public String toString() {
			return a + " at " + element;
		}

		@Override
		ThirdLevelNode self() {
			return this;
		}

		public Image getImage() {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_DEC_FIELD_WARNING);
		}
		
		@Override
		public SourceRefElement getSourceRef() {
			return null;
		}

		public IFCAnnotation getAnnotation() {
			return a;
		}

		public AnnotationTypeBasedNodeCollector getCollector() {
			return collector;
		}
	}
	
	public static class IFCInfoNode extends TreeNode<IFCInfoNode, SecondLevelNode, RootNode> {
		private final IFCResult result;
		private final IJavaProject project;
		private SearchHelper search = null;
		private IMarker[] sideMarker = null;
		
		private IFCInfoNode(final RootNode parent, final IFCResult result, final IJavaProject project) {
			super(parent);
			this.result = result;
			this.project = project;
			// TODO: replace by proper use of some Collection
			IFCInfoNode oldChildWithSameEntryPointConfiguration = null;
			Collection<IFCInfoNode> supersededChildren = new LinkedList<>();
			for (IFCInfoNode oldChild : parent.getChildren()) {
				if (oldChild.getResult().getEntryPointConfiguration() == result.getEntryPointConfiguration()
				    && oldChild != this) {
					oldChildWithSameEntryPointConfiguration = oldChild;
				}
				if (oldChild.getResult().getEntryPointConfiguration().replaces(result.getEntryPointConfiguration())) {
					supersededChildren.add(this);
				} else if (result.getEntryPointConfiguration().replaces(oldChild.getResult().getEntryPointConfiguration())) {
					supersededChildren.add(oldChild);
				}
			}
			parent.removeChild(oldChildWithSameEntryPointConfiguration);
			for (IFCInfoNode node : supersededChildren) {
				parent.removeChild(node);
			}

		}

		public IFCResult getResult() {
			return result;
		}
		
		public String toString() {
			return "Entrypoint " + niceName(result.getEntryPoint())	+ " from project " + project.getProject().getName()
				+ " " + result.toString();
		}
		
		private static String niceName(final IMethod im) {
			final StringBuilder sb = new StringBuilder(im.getDeclaringType().getFullyQualifiedName()+"."+im.getElementName());
			sb.append("(");
			final String[] types = im.getParameterTypes();
			if (types != null && types.length > 0) {
				for (int i = 0; i < types.length; i++) {
					sb.append(Signature.getSignatureSimpleName(types[i]));
					if (i + 1 < types.length) {
						sb.append(",");
					}
				}
			}
			sb.append(")");
			
			return sb.toString();
		}

		@Override
		public void searchMatchingJavaElement() {
			search = SearchHelper.create(project);
		}

		@Override
		public SourceRefElement getSourceRef() {
			return result.getEntryPointConfiguration().getEntryPointSourceRef();
		}

		@Override
		public Image getImage() {
			return EasyIFCMarkerAndImageManager.getInstance().getImage(result);
		}

		@Override
		public IMarker[] getSideMarker() {
			return sideMarker;
		}

		@Override
		public IJavaProject getProject() {
			return project;
		}

		@Override
		IFCInfoNode self() {
			return this;
		}
	}
	
	public static class NotRunYetNode extends IFCInfoNode {
		
		private NotRunYetNode(final RootNode parent, final EntryPointConfiguration discovered,
				final IJavaProject project) {
			super(parent, new IFCResult(discovered, null, null) {
				@Override
				public String toString() {
					return "has not been analysed, yet.";
				}},
				project);
			for (String error : discovered.getErrors()) {
				new EntryPointErrorNode(this, discovered, error);
			}
		}

		@Override
		public Image getImage() {
			return EasyIFCMarkerAndImageManager.getInstance().getImage(getResult().getEntryPoint());
		}
	}
	
	public static final class EntryPointErrorNode extends SecondLevelNode {
		private final EntryPointConfiguration configuration;
		private final String error;
		
		public EntryPointErrorNode(IFCInfoNode parent, EntryPointConfiguration configuration, String error) {
			super(parent);
			this.configuration = configuration;
			this.error = error;
		}
		
		@Override
		public SourceRefElement getSourceRef() {
			return configuration.getSourceRef();
		}

		@Override
		public IMarker[] getSideMarker() {
			return null;
		}

		@Override
		public void searchMatchingJavaElement() {
		}

		@Override
		public String toString() {
			return error;
		}

		@Override
		SecondLevelNode self() {
			return this;
		}

		public Image getImage() {
			return EasyIFCMarkerAndImageManager.getInstance().getImage(configuration);
		}
	}
	
	public static final class TriggerInfoNode extends ThirdLevelNode {

		private final SPos trigger;
		private IMarker[] marker = null;
		
		private TriggerInfoNode(final LeakInfoNode parent, final SPos trigger) {
			super(parent);
			this.trigger = trigger;
		}
		
		@Override
		TriggerInfoNode self() {
			return this;
		}

		@Override
		public SourceRefElement getSourceRef() {
			return null;
		}

		@Override
		public IMarker[] getSideMarker() {
			return marker;
		}

		@Override
		public void searchMatchingJavaElement() {
			final LeakInfoNode p = (LeakInfoNode) getParent();
			final SearchHelper search = p.getIFCInfo().search;
			final IMarker triggerMarker = search.createSideMarker(trigger, "causing " + p.toString(), Marker.INTERFERENCE_TRIGGER);
			if (triggerMarker != null) {
				marker = new IMarker[1];
				marker[0] = triggerMarker;
			}
		}

		@Override
		public Image getImage() {
			return EasyIFCMarkerAndImageManager.getInstance().getTriggerImage();
		}
		
		@Override
		public String toString() {
			return "caused by secret in '" + trigger.getSourceFile() + ":" + trigger.getStartLine() + "'";
		}
		
	}

	public static final class LeakInfoNode extends SecondLevelNode {
		private final SLeak leak;
		private IMarker sideMarker[] = null;

		private LeakInfoNode(final IFCInfoNode parent, final SLeak leak) {
			super(parent);
			this.leak = leak;
		}

		public String toString() {
			return leak.toString();
		}

		@Override
		public void searchMatchingJavaElement() {
			final SearchHelper search = getIFCInfo().search;
			
			if (search != null) {
				sideMarker = new IMarker[2];
				switch (leak.getReason()) {
				case DIRECT_FLOW:
				case INDIRECT_FLOW:
				case BOTH_FLOW:
				case EXCEPTION:
					sideMarker[0] = search.createSideMarker(leak.getSource(), "Secret source of illegal flow.", Marker.SECRET_INPUT);
					sideMarker[1] = search.createSideMarker(leak.getSink(), "Public sink of illegal flow.", Marker.PUBLIC_OUTPUT);
					break;
				case THREAD:
				case THREAD_DATA:
				case THREAD_ORDER:
				case THREAD_EXCEPTION:
					sideMarker[0] = search.createSideMarker(leak.getSource(), "Statement part of critical interference.", Marker.CRITICAL_INTERFERENCE);
					sideMarker[1] = search.createSideMarker(leak.getSink(), "Statement part of critical interference.", Marker.CRITICAL_INTERFERENCE);
					break;
				case UNKNOWN: //no action
					break;
				}
				
				for (final ThirdLevelNode child : getChildren()) {
					child.searchMatchingJavaElement();
				}
			}
		}
		
		public SLeak getLeak() {
			return leak;
		}

		public IFCInfoNode getIFCInfo() {
			return (IFCInfoNode) getParent();
		}

		@Override
		public SourceRefElement getSourceRef() {
			return null;
		}

		@Override
		public IMarker[] getSideMarker() {
			return sideMarker;
		}

		@Override
		public Image getImage() {
			return EasyIFCMarkerAndImageManager.getInstance().getImage(leak);
		}

		@Override
		SecondLevelNode self() {
			return this;
		}

	}

	public Object getRoot() {
		return root;
	}
 }
