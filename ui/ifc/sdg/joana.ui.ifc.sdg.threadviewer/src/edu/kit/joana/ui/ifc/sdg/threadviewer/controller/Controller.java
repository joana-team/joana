/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.threadviewer.controller;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.internal.resources.File;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.BlockTextSelection;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.zest.core.viewers.EntityConnectionData;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ui.ifc.sdg.threadviewer.model.SDGWrapper;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.Activator;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ThreadViewer;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ViewCategory;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ViewInterferedNode;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ViewInterferenceCategory;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ViewParallelCategory;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ViewParallelRegion;


/**
 * The Controller class serves as the interface between edu.kit.joana.ui.ifc.sdg.threadviewer.model and edu.kit.joana.ui.ifc.sdg.threadviewer.view.
 *
 * @author Le-Huan Stefan Tran
 */
@SuppressWarnings("restriction")
public class Controller {

	private static Controller instance;
	private SDGWrapper sdgWrapper;
	// Do not hold an instance of TreeView because TreeView is often newly created during lifetime

	// Helper map to determine all parents of nodes
	private class NodeParents {
		public NodeParents(SDGNode newNode, ViewInterferenceCategory interferenceCategory) {
			node = newNode;
			parents.add(interferenceCategory);
		}
		SDGNode node;
		Collection<ViewInterferenceCategory> parents = new HashSet<ViewInterferenceCategory>();
	}
	Collection<NodeParents> nodeParents = new HashSet<NodeParents>();

	private Image imageStatusBar;

	/* Private constructor to enforce singleton pattern */
	private Controller() {
		sdgWrapper = SDGWrapper.getInstance();

		ImageRegistry ir = Activator.getDefault().getImageRegistry();
		ImageDescriptor desc = ir.getDescriptor(Activator.IMAGE_STATUSBAR);
		imageStatusBar = desc.createImage();
	}

	/**
	 * Gets the instance of the Controller class.
	 * Implements the Singleton pattern.
	 *
	 * @return	The instance of the Controller class
	 */
	public static Controller getInstance() {
		if (instance == null) {
			instance = new Controller();
		}
		return instance;
	}


	/**
	 * Updates the status bar of eclipse with the given text.
	 *
	 * @param text	The text to be shown
	 */
	public void updateStatusBar(String text) {
		IWorkbenchPartSite site = PlatformUI.getWorkbench().getActiveWorkbenchWindow().
		getActivePage().getActivePart().getSite();

		if (site instanceof IViewSite) {
			IViewSite vSite = (IViewSite) site;
			IStatusLineManager status = vSite.getActionBars().getStatusLineManager();
			status.setMessage(this.imageStatusBar, text);
		} else if (site instanceof IEditorSite) {
			IEditorSite eSite = (IEditorSite) site;
			IStatusLineManager status = eSite.getActionBars().getStatusLineManager();
			status.setMessage(this.imageStatusBar, text);
		}
	}


	/* Package Explorer handling */

	/**
	 * Method to handle the loading process of a new .pdg-file.
	 */
	public void runPackageExplorerFollowThreads(IAction action, ISelection selection) {
		this.checkView();

		if (selection instanceof TreeSelection) {
			TreeSelection treeSelection = (TreeSelection) selection;
			Object obj = treeSelection.getFirstElement();

			if (obj instanceof File) {
				File file = (File) obj;
				sdgWrapper.changeModel(file.getFullPath());

				ThreadViewer.getInstance().setInitialInput(file.getFullPath());
				ThreadViewer.getInstance().refresh();
			}
		}
	}


	/* Editor handling */

	/**
	 * Method to handle the follow process of the given thread region started
	 * by an editor action.
	 */
	public void runEditorFollowThreadRegion(IEditorPart targetPart) {
		// Ensure ThreadViewer to be accessible
		this.checkView();

		// Ensure .pdg-file is correctly loaded and calculations are done
		if (sdgWrapper.isCreated()) {
			ThreadRegion region = this.getSelectedRegion(targetPart);

			if (region != null) {
				// Open corresponding file(s)
				this.openFiles(region);
				// Highlight region
				this.followObject(region);
				// Set selected region in ThreadViewer
				ThreadViewer.getInstance().setSelectedObject(region);
			} else {
				ThreadViewer.getInstance().clearAllHighlights();
				this.updateStatusBar("Selection is not part of loaded .pdg-file.");
			}
		} else {
			this.updateStatusBar("Load .pdg-file into ThreadViewer first.");
		}
	}

	/**
	 * Method to handle the follow process of parallel regions of
	 * the given thread region started by an editor action.
	 */
	public void runEditorFollowParallelRegions(IEditorPart targetPart) {
		this.checkView();

		if (sdgWrapper.isCreated()) {
			ThreadRegion region = this.getSelectedRegion(targetPart);

			if (region != null) {
				if (sdgWrapper.getParallelRegions(region).size() > 0) {
					this.openFiles(region);
					this.followParallelObjects(region);
					ThreadViewer.getInstance().setParallelObjects(region);
					ThreadViewer.getInstance().setFocus();
				} else {
					ThreadViewer.getInstance().clearAllHighlights();
					this.updateStatusBar("Selection has no parallel regions.");
				}
			} else {
				ThreadViewer.getInstance().clearAllHighlights();
				this.updateStatusBar("Selection is not part of loaded .pdg-file.");
			}
		} else {
			this.updateStatusBar("Load .pdg-file into ThreadViewer first.");
		}
	}

	/**
	 * Method to handle the follow process of the given thread started
	 * by an editor action.
	 */
	public void runEditorFollowThread(IEditorPart targetPart) {
		this.checkView();

		if (sdgWrapper.isCreated()) {
			ThreadRegion region = this.getSelectedRegion(targetPart);

			if (region != null) {
				ThreadInstance thread = sdgWrapper.getThread(region);

				this.openFiles(thread);
				this.followObject(thread);
				ThreadViewer.getInstance().setSelectedObject(thread);
			} else {
				ThreadViewer.getInstance().clearAllHighlights();
				this.updateStatusBar("Selection is not part of loaded .pdg-file.");
			}
		} else {
			this.updateStatusBar("Load .pdg-file into ThreadViewer first.");
		}
	}

	/**
	 * Method to handle the follow process of the given node started
	 * by an editor action.
	 */
	public void runEditorFollowNode(IEditorPart targetPart) {
		this.checkView();

		if (sdgWrapper.isCreated()) {
			SDGNode node = this.getSelectedNode(targetPart);

			if (node != null) {
				this.followObject(node);
				ThreadViewer.getInstance().setSelectedObject(node);
			} else {
				ThreadViewer.getInstance().clearAllHighlights();
				this.updateStatusBar("Selection is not part of loaded .pdg-file.");
			}
		} else {
			this.updateStatusBar("Load .pdg-file into ThreadViewer first.");
		}
	}

	/**
	 * Method to handle the follow process of interfered nodes of
	 * the given node started by an editor action.
	 */
	public void runEditorFollowInterferedNodes(IEditorPart targetPart) {
		this.checkView();

		if (sdgWrapper.isCreated()) {
			SDGNode node = this.getSelectedNode(targetPart);

			if (node != null) {
				this.followInterferedObjects(node);
				ThreadViewer.getInstance().setParallelObjects(node);
			} else {
				ThreadViewer.getInstance().clearAllHighlights();
				this.updateStatusBar("Selection is not part of loaded .pdg-file.");
			}
		} else {
			this.updateStatusBar("Load .pdg-file into ThreadViewer first.");
		}
	}


	/* Tree View handling */

	/**
	 * Method to handle a double click in the plug-in edu.kit.joana.ui.ifc.sdg.threadviewer.view started
	 * by a edu.kit.joana.ui.ifc.sdg.threadviewer.view action.
	 */
	public void runViewDoubleClick(Object obj) {
		if (obj != null) {
			if (obj instanceof ViewCategory) {
				ViewCategory category = (ViewCategory) obj;
				ThreadViewer.getInstance().doubleclickNode(category);
			} else {
				boolean success = this.followObject(obj);

				if (success) {
					this.openFiles(obj);
					this.setCursor(obj);
					ThreadViewer.getInstance().setFocus();
				}
			}
		}
	}

	/**
	 * Method to handle the follow process of the given node started
	 * by a edu.kit.joana.ui.ifc.sdg.threadviewer.view action.
	 */
	public void runViewFollowThreadRegion() {
		Object obj = ThreadViewer.getInstance().getSelectedObject();

		if (obj != null) {
			boolean success = this.followObject(obj);

			if (success) {
				this.openFiles(obj);
				this.setCursor(obj);
				ThreadViewer.getInstance().setFocus();
			}
		}
	}

	/**
	 * Method to handle the follow process of parallel nodes of
	 * the given node started by a edu.kit.joana.ui.ifc.sdg.threadviewer.view action.
	 */
	public void runViewFollowParallelRegion() {
		Object obj = ThreadViewer.getInstance().getSelectedObject();

		if (obj != null) {
			boolean success = this.followParallelObjects(obj);

			if (success) {
				this.openFiles(obj);
				this.setCursor(obj);
				ThreadViewer.getInstance().setParallelObjects(obj);
				ThreadViewer.getInstance().setFocus();
			}
		}
	}

	/**
	 * Method to handle the follow process of the given thread started
	 * by a edu.kit.joana.ui.ifc.sdg.threadviewer.view action.
	 */
	public void runViewFollowThread() {
		Object obj = ThreadViewer.getInstance().getSelectedObject();

		if (obj != null) {
			boolean success = this.followObject(obj);

			if (success) {
				this.openFiles(obj);
				this.setCursor(obj);
				ThreadViewer.getInstance().setFocus();
			}
		}
	}

	/**
	 * Method to handle the center process of the given parallel region
	 * started by a edu.kit.joana.ui.ifc.sdg.threadviewer.view action.
	 */
	public void runViewCenterParallelRegion() {
		Object obj = ThreadViewer.getInstance().getSelectedObject();

		if (obj != null) {
			boolean success = this.followObject(obj);

			if (success) {
				this.openFiles(obj);
				this.setCursor(obj);
				ThreadViewer.getInstance().centerView(obj);
				ThreadViewer.getInstance().setFocus();
			}
		}
	}

	/**
	 * Method to handle the follow process of the given node
	 * started by a edu.kit.joana.ui.ifc.sdg.threadviewer.view action.
	 */
	public void runViewFollowNode() {
		Object obj = ThreadViewer.getInstance().getSelectedObject();

		if (obj != null) {
			boolean success = this.followObject(obj);

			if (success) {
				this.openFiles(obj);
				this.setCursor(obj);
				ThreadViewer.getInstance().setFocus();
			}
		}
	}

	/**
	 * Method to handle the follow process of the given interfered node
	 * started by a edu.kit.joana.ui.ifc.sdg.threadviewer.view action.
	 */
	public void runViewFollowInterferedNodes() {
		Object obj = ThreadViewer.getInstance().getSelectedObject();

		// Dirty workaround to handle interference edges in graph
		if (obj instanceof EntityConnectionData) {
			this.runViewFollowInterference();
			return;
		}

		if (obj != null) {
			boolean success = this.followInterferedObjects(obj);

			if (success) {
				this.openFiles(obj);
				this.setCursor(obj);
				ThreadViewer.getInstance().setParallelObjects(obj);
				ThreadViewer.getInstance().setFocus();
			}
		}
	}

	/**
	 * Method to handle the center process of the given interfered node
	 * started by a edu.kit.joana.ui.ifc.sdg.threadviewer.view action.
	 */
	public void runViewCenterInterferedNode() {
		Object obj = ThreadViewer.getInstance().getSelectedObject();

		if (obj != null) {
			boolean success = this.followObject(obj);

			if (success) {
				this.openFiles(obj);
				this.setCursor(obj);
				ThreadViewer.getInstance().centerView(obj);
				ThreadViewer.getInstance().setFocus();
			}
		}
	}

	/**
	 * Method to handle the follow process of the given interference
	 * started by a edu.kit.joana.ui.ifc.sdg.threadviewer.view action.
	 */
	public void runViewFollowInterference() {
		Object obj = ThreadViewer.getInstance().getSelectedObject();

		if (obj != null) {
			boolean success = this.followObject(obj);

			if (success) {
				this.openFiles(obj);
				this.setCursor(obj);
				ThreadViewer.getInstance().setFocus();
			}
		}
	}

	/**
	 * Change the SDG to be shown for the given java project.
	 * @param sdg the sdg to be shown
	 * @param jProject the java project in which the sdg was changed
	 */
	public void changeModel(SDG sdg, IJavaProject jProject) {
		sdgWrapper.changeModel(sdg, jProject);
	}



	/**
	 * Handle a selection change of the edu.kit.joana.ui.ifc.sdg.threadviewer.view.
	 *
	 * @param selection	The new selection
	 */
	public void selectionChanged(ISelection selection) {
		ThreadViewer.getInstance().selectionChanged(selection);
	}


	/**
	 * Gets the root elements of the tree.
	 */
	public Object[] getTreeRoots() {
		return sdgWrapper.getThreads().toArray();
	}

	/**
	 * Return the children of the given object of the tree.
	 *
	 * @param parent	The parent object whose children are to be returned
	 * @return			The children
	 */
	public Object[] getTreeChildren(Object parent) {
		if (parent instanceof ThreadInstance) {
			Object[] regions = sdgWrapper.getRegions((ThreadInstance) parent).toArray();
			return regions;
		} else if (parent instanceof ThreadRegion) {
			ThreadRegion region = (ThreadRegion) parent;
			Object[] categories = null;

			if (sdgWrapper.getParallelRegions(region).size() > 0) {
				if (sdgWrapper.getInterferingNodes(region).size() > 0) {
					categories = new ViewCategory[2];
					categories[0] = new ViewParallelCategory(region);
					categories[1] = new ViewInterferenceCategory(region);
				} else {
					categories = new ViewCategory[1];
					categories[0] = new ViewParallelCategory(region);
				}
			}

			return categories;
		} else if (parent instanceof ViewParallelCategory) {
			ViewParallelCategory parallelCategory = (ViewParallelCategory) parent;
			Collection<ViewParallelRegion> parallelItems = new HashSet<ViewParallelRegion>();

			// Boxing of parallel Thread Regions
			for (ThreadRegion region : sdgWrapper.getParallelRegions(parallelCategory.getParent())) {
				parallelItems.add(new ViewParallelRegion(parallelCategory, region));
			}

			return parallelItems.toArray();
		} else if (parent instanceof ViewInterferenceCategory) {
			ViewInterferenceCategory interferenceCategory = (ViewInterferenceCategory) parent;
			Collection<SDGNode> interferingNodes = new HashSet<SDGNode>();

			// Boxing of interfering Nodes
			for (SDGNode node :	sdgWrapper.getInterferingNodes(interferenceCategory.getParent())) {
				interferingNodes.add(node);

				// Add parent to helper set
				boolean found = false;
				for (NodeParents np : nodeParents) {
					if (np.node.equals(node)) {
						np.parents.add(interferenceCategory);
						found = true;
					}
				}
				if (!found) {
					nodeParents.add(new NodeParents(node, interferenceCategory));
				}
			}

			return interferingNodes.toArray();
		} else if (parent instanceof SDGNode) {
			SDGNode interferingNode = (SDGNode) parent;
			Collection<ViewInterferedNode> interferedNodes = new HashSet<ViewInterferedNode>();

			// Boxing of interfered Nodes
			for (SDGNode node :	sdgWrapper.getInterferedNodes(interferingNode)) {
				interferedNodes.add(new ViewInterferedNode(interferingNode, node));
			}

			return interferedNodes.toArray();
		}
		return null;
	}

	/**
	 * Gets the parent of the given element of the tree.
	 *
	 * @param element	The element whose parent is to be returned
	 * @return			The parent
	 */
	public Object getTreeParent(Object element) {
		if (element instanceof ThreadRegion) {
			ThreadInstance thread = sdgWrapper.getThread((ThreadRegion) element);
			return thread;
		} else if (element instanceof ViewCategory) {
			ViewCategory category = (ViewCategory) element;
			return category.getParent();
		} else if (element instanceof SDGNode) {
			SDGNode node = (SDGNode) element;
			for (NodeParents np : nodeParents) {
				if (np.node.equals(node)) {
					return np.parents.toArray();
				}
			}
		} else if (element instanceof ViewInterferedNode) {
			ViewInterferedNode node = (ViewInterferedNode) element;
			return node.getParent();
		}
		return null;
	}

	/**
	 * Checks if the given tree element has at least one child.
	 *
	 * @param element	The element to be checked
	 * @return			True if the element has at least one child
	 */
	public boolean hasTreeChildren(Object element) {
		boolean hasChildren = false;

		if (element instanceof ThreadInstance) {
			ThreadInstance thread = (ThreadInstance) element;

			// Check if there are thread regions at all
			if (ThreadViewer.getInstance().isSourceCodeFilterSet()) {
				for (ThreadRegion region : sdgWrapper.getRegions(thread)) {
					if (sdgWrapper.isInSourceCode(region)) {
						hasChildren = true;
						break;
					}
				}
			} else {
				if (sdgWrapper.getRegions(thread).size() > 0) {
					hasChildren = true;
				}
			}
		} else if (element instanceof ThreadRegion) {
			ThreadRegion region = (ThreadRegion) element;

			// Check if region is valid and has at least one parallel region.
			if (sdgWrapper.isInSourceCode(region)) {
				if (sdgWrapper.getParallelRegions(region).size() > 0) {
					hasChildren = true;
				}
			}
		} else if (element instanceof ViewParallelCategory) {
			ThreadRegion region = ((ViewParallelCategory) element).getParent();

			// Check if region valid and has at least one parallel region.
			if (sdgWrapper.isInSourceCode(region)) {
				if (sdgWrapper.getParallelRegions(region).size() > 0) {
					hasChildren = true;
				}
			}
		} else if (element instanceof ViewInterferenceCategory) {
			ThreadRegion region = ((ViewInterferenceCategory) element).getParent();

			// Check if region valid and has at least one parallel region.
			if (sdgWrapper.isInSourceCode(region)) {
				if (sdgWrapper.getInterferingNodes(region).size() > 0) {
					hasChildren = true;
				}
			}
		} else if (element instanceof SDGNode) {
			SDGNode node = (SDGNode) element;

			// Check if node valid and has at least one interfered node.
			if (sdgWrapper.isInSourceCode(node)) {
				if (sdgWrapper.getInterferedNodes(node).size() > 0) {
					hasChildren = true;
				}
			}
		}
		return hasChildren;
	}

	/**
	 * Gets the roots on first start-up.
	 *
	 * @return	The roots
	 */
	public Object getInitialRoot() {
		return sdgWrapper.getThreads();
	}


	/* GraphViewer methods */

	/**
	 * Gets all elements to be displayed in the graph.
	 */
	public Object[] getGraphElements() {
		Collection<Object> elements = new HashSet<Object>();

		// Add threads
		Collection<ThreadInstance> threads = sdgWrapper.getThreads();
		elements.addAll(threads);

		// Add regions
		// Just add every region - regions will be filtered by our filters
		elements.addAll(sdgWrapper.getRegions());

		return elements.toArray();
	}

	/**
	 * Returns all elements of the graph the given object is connected to.
	 *
	 * @param entity	The object whose connected elements are to be returned
	 * @return			The connnected elements
	 */
	public Object[] getConnectedTo(Object entity) {
		Collection<ThreadRegion> targets = new HashSet<ThreadRegion>();

		if (entity != null) {
			// Get control flows
			Collection<ThreadRegion> next = null;

			if (ThreadViewer.getInstance().isSourceCodeFilterSet()) {
				if (ThreadViewer.getInstance().isInterferingFilterSet()) {
					if (ThreadViewer.getInstance().isFilterHideInterproceduralEdgesSet()) {
						next = sdgWrapper.getNextSourceCodeInterferingRegions(entity);
					} else {
						next = sdgWrapper.getNextSourceCodeInterferingRegionsWithInterprocEdges(entity);
					}
				} else {
					if (ThreadViewer.getInstance().isFilterHideInterproceduralEdgesSet()) {
						next = sdgWrapper.getNextSourceCodeRegions(entity);
					} else {
						next = sdgWrapper.getNextSourceCodeRegionsWithInterprocEdges(entity);
					}
				}
			} else {
				if (ThreadViewer.getInstance().isInterferingFilterSet()) {
					if (ThreadViewer.getInstance().isFilterHideInterproceduralEdgesSet()) {
						next = sdgWrapper.getNextInterferingRegions(entity);
					} else {
						next = sdgWrapper.getNextInterferingRegionsWithInterprocEdges(entity);
					}
				} else {
					if (ThreadViewer.getInstance().isFilterHideInterproceduralEdgesSet()) {
						next = sdgWrapper.getNextRegions(entity);
					} else {
						next = sdgWrapper.getNextRegionsWithInterprocEdges(entity);
					}
				}
			}

			if (next != null) {
				targets.addAll(next);
			}

			// Get interferences
			Collection<ThreadRegion> interfering = null;

			if (ThreadViewer.getInstance().isSourceCodeFilterSet()) {
				interfering = sdgWrapper.getSourceCodeInterferedRegions(entity);
			} else {
				interfering = sdgWrapper.getInterferedRegions(entity);
			}

			if (interfering != null) {
				// Only get forward edge
				if (entity instanceof ThreadInstance) {
					targets.addAll(interfering);
				} else if (entity instanceof ThreadRegion) {
					ThreadRegion start = (ThreadRegion) entity;

					for (ThreadRegion target : interfering) {
						if (start.getID() < target.getID()) {
							targets.add(target);
						}
					}
				}
			}
		}

		return targets.toArray();
	}


	/* Helper methods */

	private void checkView() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (page.findViewReference(ThreadViewer.THREADVIEWER_ID) == null) {
			try {
				page.showView(ThreadViewer.THREADVIEWER_ID);
			} catch (PartInitException e) {
				this.updateStatusBar("Open ThreadViewer failed.");
				e.printStackTrace();
				return;
			}
		}
	}


	/* Follows the given object in editor */
	private boolean followObject(Object obj) {
		boolean success = false;

		if (obj instanceof ThreadInstance) {
			ThreadInstance thread = (ThreadInstance) obj;
			ThreadViewer.getInstance().clearAllHighlights();

			for (ThreadRegion region : sdgWrapper.getRegions(thread)) {
				ThreadViewer.getInstance().highlightRegion(region, ThreadViewer.COLOR_THREAD);
			}

			success = true;
		} else if (obj instanceof ThreadRegion || obj instanceof ViewParallelRegion) {
			ThreadRegion region = null;

			if (obj instanceof ThreadRegion) {
				region = (ThreadRegion) obj;
			} else {
				region = ((ViewParallelRegion) obj).getItem();
			}

			if (sdgWrapper.isInSourceCode(region)) {
				// Highlight other thread regions
				ThreadViewer.getInstance().clearAllHighlights();
				ThreadViewer.getInstance().highlightRemainingThread(region);

				// Highlight selected thread region
				ThreadViewer.getInstance().highlightRegion(region, ThreadViewer.COLOR_REGION);
				success = true;
			} else {
				this.updateStatusBar("Object is not in source code.");
			}
		} else if (obj instanceof SDGNode) {
			SDGNode node = (SDGNode) obj;

			if (sdgWrapper.isInSourceCode(node)) {
				// Highlight other region
				ThreadViewer.getInstance().clearAllHighlights();
				ThreadViewer.getInstance().highlightRemainingRegion(node);

				// Highlight selected node
				ThreadViewer.getInstance().highlight(node, ThreadViewer.COLOR_NODE);
				success = true;
			} else {
				this.updateStatusBar("Object is not in source code.");
			}
		} else if (obj instanceof ViewInterferedNode) {
			SDGNode node = ((ViewInterferedNode) obj).getItem();

			if (sdgWrapper.isInSourceCode(node)) {
				// Highlight node
				ThreadViewer.getInstance().clearAllHighlights();
				ThreadViewer.getInstance().highlight(node, ThreadViewer.COLOR_NODE);
				success = true;
			} else {
				this.updateStatusBar("Object is not in source code.");
			}
		} else if (obj instanceof EntityConnectionData) {
			EntityConnectionData connection = (EntityConnectionData) obj;
			Object tmpSource = connection.source;
			Object tmpTarget = connection.dest;

			if (tmpSource instanceof ThreadRegion && tmpTarget instanceof ThreadRegion) {
				ThreadRegion source = (ThreadRegion) tmpSource;
				ThreadRegion target = (ThreadRegion) tmpTarget;

				boolean sourceInSourceCode = false;
				boolean targetInSourceCode = false;
				ThreadViewer.getInstance().clearAllHighlights();

				if (sdgWrapper.isInSourceCode(source)) {
					Collection<SDGNode> interferingNodes = sdgWrapper.getInterferingNodes(source);
					ThreadViewer.getInstance().highlight(
							interferingNodes, ThreadViewer.COLOR_THREADS[source.getThread() % 4]);
					sourceInSourceCode = true;
				}

				if (sdgWrapper.isInSourceCode(target)) {
					Collection<SDGNode> interferedNodesCandidates =
						sdgWrapper.getInterferedNodes(source);
					Collection<SDGNode> interferedNodes = new HashSet<SDGNode>();

					for (SDGNode candidate : interferedNodesCandidates) {
						if (target.contains(candidate)) {
							interferedNodes.add(candidate);
						}
					}

					ThreadViewer.getInstance().highlight(
							interferedNodes, ThreadViewer.COLOR_THREADS[target.getThread() % 4]);
					targetInSourceCode = true;
				}


				if (!sourceInSourceCode && !targetInSourceCode) {
					this.updateStatusBar("Both objects are not in source code.");
				} else if (!sourceInSourceCode || !targetInSourceCode) {
					this.updateStatusBar("One object is not in source code.");
				} else {
					this.updateStatusBar("");
					success = true;
				}
			}
		}

		return success;
	}

	/* Follows the parallel objects of the given object in editor */
	private boolean followParallelObjects(Object obj) {
		boolean success = false;

		if (obj instanceof ThreadInstance) {
			// do nothing
		} else if (obj instanceof ThreadRegion || obj instanceof ViewParallelRegion) {
			ThreadRegion region = null;

			if (obj instanceof ThreadRegion) {
				region = (ThreadRegion) obj;
			} else {
				region = ((ViewParallelRegion) obj).getItem();
			}

			if (sdgWrapper.isInSourceCode(region)) {
				// Highlight the selected region
				ThreadViewer.getInstance().clearAllHighlights();
				ThreadViewer.getInstance().highlightRegion(
						region, ThreadViewer.COLOR_REMAINING_REGION);

				// Highlight the parallel regions
				for (ThreadRegion tempRegion : sdgWrapper.getParallelRegions(region)) {
					ThreadViewer.getInstance().highlightRegion(
							tempRegion, ThreadViewer.COLOR_PARALLEL);
				}
				success = true;
			} else {
				this.updateStatusBar("Object is not in source code.");
			}
		}

		return success;
	}

	/* Follows the objects interfered by the given object in editor */
	private boolean followInterferedObjects(Object obj) {
		boolean success = false;

		if (obj instanceof SDGNode || obj instanceof ViewInterferedNode) {
			SDGNode node = null;

			if (obj instanceof SDGNode) {
				node = (SDGNode) obj;
			} else {
				node = ((ViewInterferedNode) obj).getItem();
			}

			if (sdgWrapper.isInSourceCode(node)) {
				// Highlight interfered nodes
				ThreadViewer.getInstance().clearAllHighlights();
				ThreadViewer.getInstance().highlightInterferedNodes(
						node, ThreadViewer.COLOR_INTERFERED);

				// Highlight selected node
				ThreadViewer.getInstance().highlight(node, ThreadViewer.COLOR_INTERFERING);
				success = true;
			} else {
				this.updateStatusBar("Object is not in source code.");
			}
		}

		return success;
	}

	/* Returns the node selected by the user in source code 					*/
	/* If more than one node is available, only take the first suitable.		*/
	private SDGNode getSelectedNode(IEditorPart targetPart) {
		// Get user selection
		IEditorSite site = targetPart.getEditorSite();
		ISelectionProvider selectionProvider = site.getSelectionProvider();
		ISelection selection = selectionProvider.getSelection();
		int startRow = -1;
		int endRow = -1;

		if (selection instanceof ITextSelection) {
			ITextSelection selectionText = (ITextSelection) selection;
			startRow = selectionText.getStartLine() + 1;
			endRow = selectionText.getEndLine() + 1;

			String currentFile = targetPart.getTitle();

			// Loop through all available regions
			for (ThreadRegion region : sdgWrapper.getRegions()) {
				// Check if region is in source code
				if (sdgWrapper.isInSourceCode(region)) {

					// Loop through all available nodes of one region
					for (SDGNode node : region.getNodes()) {
						// Check if node is in correct file
						// (optional: currentFile: check with current path)
						if (node.getSource() != null) {
							if(node.getSource().endsWith(currentFile)) {
								// Check if correct code snippet
								if (node.getSr() <= startRow && endRow <= node.getEr()) {
									// Check if node has interferences, i.e. is interesting
									if (sdgWrapper.getInterferedNodes(node).size() > 0) {
										return node;
									}
								}
							}
						}
					}
				}
			}
		}

		return null;
	}

	/* Returns the region selected by the user in source code 					*/
	/* Take the first thread in source code corresponding to the selected node.	*/
	/* All other threads are omitted.											*/
	private ThreadRegion getSelectedRegion(IEditorPart targetPart) {
		// Get user selection
		IEditorSite site = targetPart.getEditorSite();
		ISelectionProvider selectionProvider = site.getSelectionProvider();
		ISelection selection = selectionProvider.getSelection();
		int startRow = -1;
		int endRow = -1;

		if (selection instanceof ITextSelection) {
			ITextSelection selectionText = (ITextSelection) selection;
			startRow = selectionText.getStartLine() + 1;
			endRow = selectionText.getEndLine() + 1;

			String currentFile = targetPart.getTitle();

			// Loop through all available regions
			for (ThreadRegion region : sdgWrapper.getRegions()) {
				// Check if region is in source code
				if (sdgWrapper.isInSourceCode(region)) {

					// Loop through all available nodes of one region
					for (SDGNode node : region.getNodes()) {
						// Check if node is in correct file
						// (optional: currentFile: check with current path)
						if (node.getSource() != null) {
							if(node.getSource().endsWith(currentFile)) {
								// Check if correct code snippet
								if (node.getSr() <= startRow && endRow <= node.getEr()) {
									return region;
								}
							}
						}
					}
				}
			}
		}

		return null;
	}


	/* Open files according to the given object */
	private void openFiles(Object obj) {
		final Collection<? extends SDGNode> nodes;

		// Collect all nodes of the given object
		if (obj instanceof ThreadInstance) {
			ThreadInstance thread = (ThreadInstance) obj;
			nodes = sdgWrapper.getNodes(thread);
		} else if (obj instanceof ThreadRegion || obj instanceof ViewParallelRegion) {
			ThreadRegion region = null;

			if (obj instanceof ThreadRegion) {
				region = (ThreadRegion) obj;
			} else {
				region = ((ViewParallelRegion) obj).getItem();
			}

			nodes = region.getNodes();
		} else if (obj instanceof SDGNode || obj instanceof ViewInterferedNode) {
			SDGNode node = null;

			if (obj instanceof SDGNode) {
				node = (SDGNode) obj;
			} else {
				node = ((ViewInterferedNode) obj).getItem();
			}

			final Set<SDGNode> nnodes = new HashSet<>();
			nnodes.add(node);
			nnodes.addAll(sdgWrapper.getInterferedNodes(node));
			nodes = nnodes;
		} else if (obj instanceof EntityConnectionData) {
			EntityConnectionData connection = (EntityConnectionData) obj;
			Object tmpSource = connection.source;
			Object tmpTarget = connection.dest;
			
			final Set<SDGNode> nnodes = new HashSet<>();

			if (tmpSource instanceof ThreadRegion && tmpTarget instanceof ThreadRegion) {
				ThreadRegion source = (ThreadRegion) tmpSource;
				ThreadRegion target = (ThreadRegion) tmpTarget;

				nnodes.addAll(source.getNodes());
				nnodes.addAll(target.getNodes());
			}
			
			nodes = nnodes;
		} else {
			// Given object is invalid
			return;
		}

		// Collect all source files
		Collection<String> sources = new HashSet<String>();

		for (SDGNode node : nodes) {
			if (sdgWrapper.isInSourceCode(node)) {
				sources.add(node.getSource());
			}
		}

		// Open files
		for (String source : sources) {
			this.openFile(source);
		}
	}

	/* Opens the given file */
	private IEditorPart openFile(String source) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IProject project = sdgWrapper.getProject();

		source = "src/" + source;
		IFile file = project.getFile(source);
		IEditorPart editorPart = null;

		if (file.exists()){
			IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().
			getDefaultEditor(file.getName());
			try {
				editorPart = page.openEditor(new FileEditorInput(file), desc.getId());
			} catch (PartInitException e) {
				this.updateStatusBar("File could not be opened.");
				e.printStackTrace();
			}
		}

		return editorPart;
	}

	/* Sets the mouse cursor to the given object */
	private void setCursor(Object obj) {
		SDGNode nodeToSet = null;

		// Get first node of object in valid source code
		if (obj instanceof ThreadInstance) {
			ThreadInstance thread = (ThreadInstance) obj;
			Collection<SDGNode> nodes = sdgWrapper.getNodes(thread);
			if (!nodes.isEmpty()) {
				nodeToSet = this.getValidNode(nodes);
			}
		} else if (obj instanceof ThreadRegion || obj instanceof ViewParallelRegion) {
			ThreadRegion region = null;

			if (obj instanceof ThreadRegion) {
				region = (ThreadRegion) obj;
			} else {
				region = ((ViewParallelRegion) obj).getItem();
			}

			// Check if given region is valid
			if (!sdgWrapper.isInSourceCode(region)) {
				this.updateStatusBar("Thread region is not in source code.");
				return;
			} else {
				Collection<? extends SDGNode> nodes = region.getNodes();
				nodeToSet = this.getValidNode(nodes);
			}
		} else if (obj instanceof SDGNode) {
			nodeToSet = (SDGNode) obj;
		} else if (obj instanceof ViewInterferedNode) {
			nodeToSet = ((ViewInterferedNode) obj).getItem();
		} else if (obj instanceof EntityConnectionData) {
			EntityConnectionData connection = (EntityConnectionData) obj;
			Object tmpSource = connection.source;

			if (tmpSource instanceof ThreadRegion) {
				ThreadRegion source = (ThreadRegion) tmpSource;

				nodeToSet = this.getValidNode(source.getNodes());
			}
		} else {
			// Given object is invalid
			return;
		}

		if (nodeToSet != null) {
			IEditorPart editorPart = this.openFile(nodeToSet.getSource());

			if (editorPart != null) {
				IEditorSite editorSite = editorPart.getEditorSite();
				IEditorInput editorInput = editorPart.getEditorInput();
				ISelectionProvider selectionProvider = editorSite.getSelectionProvider();

				// Get selection
				int sourceRow = 0;
				int sourceCol = 0;
				sourceRow = nodeToSet.getSr();
				sourceCol = nodeToSet.getSc();

				if (sourceRow > 0) {
					sourceRow--;
				}

				IDocument document = ((ITextEditor) editorPart).getDocumentProvider().
				getDocument(editorInput);
				ISelection newSelection = new BlockTextSelection(document,
						sourceRow, sourceCol,
						sourceRow, sourceCol, 4);
				selectionProvider.setSelection(newSelection);
			}
		}
	}

	private SDGNode getValidNode(Collection<? extends SDGNode> nodes) {
		Iterator<? extends SDGNode> iterNodes = nodes.iterator();
		SDGNode validNode = null;
		boolean found = false;

		// Check if node is valid
		while (iterNodes.hasNext() && !found) {
			SDGNode node = iterNodes.next();

			if (sdgWrapper.isInSourceCode(node)) {
				validNode = node;
				found = true;
			}
		}

		return validNode;
	}
}
