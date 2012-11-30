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
package edu.kit.joana.ui.ifc.sdg.threadviewer.view;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "ThreadViewer";

	// The shared instance
	private static Activator plugin;

	/**
	 * The string constant for the shared JOANA nature icon
	 */
	public static final String IMAGE_LOGO = "Logo";
	public static final String IMAGE_TREEVIEW_THREAD = "TreeView_Thread";
	public static final String IMAGE_TREEVIEW_THREADREGION = "TreeView_Item";
	public static final String IMAGE_TREEVIEW_THREADREGION_DISABLED = "TreeView_ItemDisabled";
	public static final String IMAGE_TREEVIEW_PARALLEL_CATEGORY = "TreeView_ParallelCategory";

	public static final String IMAGE_TREEVIEW_INTERFERENCE_CATEGORY = "TreeView_InterferenceCategory";
	public static final String IMAGE_TREEVIEW_INTERFERING_NODE = "TreeView_InterferingNode";
	public static final String IMAGE_TREEVIEW_INTERFERING_NODE_DISABLED = "TreeView_InterferingNode_Disabled";
	public static final String IMAGE_TREEVIEW_INTERFERED_NODE = "TreeView_InterferedNode";
	public static final String IMAGE_TREEVIEW_INTERFERED_NODE_DISABLED = "TreeView_InterferedNode_Disabled";

	public static final String IMAGE_VIEW_CONTEXTMENU_FOLLOW_THREAD_REGION = "ViewContextmenu_FollowThreadRegion";
	public static final String IMAGE_VIEW_CONTEXTMENU_FOLLOW_PARALLEL_REGIONS = "ViewContextmenu_FollowParallelRegions";
	public static final String IMAGE_VIEW_CONTEXTMENU_CENTER_REGION = "ViewContextmenu_CenterRegion";
	public static final String IMAGE_VIEW_CONTEXTMENU_FOLLOW_THREAD = "ViewContextmenu_FollowThread";
	public static final String IMAGE_VIEW_CONTEXTMENU_FOLLOW_NODE = "ViewContextmenu_FollowNode";
	public static final String IMAGE_VIEW_CONTEXTMENU_FOLLOW_INTERFERED_NODES = "ViewContextmenu_FollowInterferedNodes";
	public static final String IMAGE_VIEW_CONTEXTMENU_CENTER_NODE = "ViewContextmenu_CenterNode";
	public static final String IMAGE_VIEW_CONTEXTMENU_FOLLOW_INTERFERENCE = "ViewContextmenu_FollowInterference";

	public static final String IMAGE_VIEW_TOOLBAR_FOLLOW_THREAD_REGION = "ViewToolbar_FollowThreadRegion";
	public static final String IMAGE_VIEW_TOOLBAR_FOLLOW_PARALLEL_REGIONS = "ViewToolbar_FollowParallelRegions";
	public static final String IMAGE_VIEW_TOOLBAR_CENTER_REGION = "ViewToolbar_CenterRegion";
	public static final String IMAGE_VIEW_TOOLBAR_FOLLOW_THREAD = "ViewToolbar_FollowThread";
	public static final String IMAGE_VIEW_TOOLBAR_FOLLOW_NODE = "ViewToolbar_FollowNode";
	public static final String IMAGE_VIEW_TOOLBAR_FOLLOW_INTERFERED_NODES = "ViewToolbar_FollowInterferedNodes";
	public static final String IMAGE_VIEW_TOOLBAR_CENTER_NODE = "ViewToolbar_CenterNode";

	public static final String IMAGE_STATUSBAR = "StatusBar";


	/**
	 * The constructor
	 */
	public Activator() { }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry ir) {
		String[][] images = {	{IMAGE_LOGO, "icons/ThreadViewer_Logo.png"},
								{IMAGE_TREEVIEW_THREAD, "icons/TreeView_Thread.png"},
								{IMAGE_TREEVIEW_THREADREGION, "icons/TreeView_ThreadRegion.png"},
								{IMAGE_TREEVIEW_THREADREGION_DISABLED, "icons/TreeView_ThreadRegionDisabled.png"},
								{IMAGE_TREEVIEW_PARALLEL_CATEGORY, "icons/TreeView_ParallelCategory.png"},

								{IMAGE_TREEVIEW_INTERFERENCE_CATEGORY, "icons/TreeView_InterferenceCategory.png"},
								{IMAGE_TREEVIEW_INTERFERING_NODE, "icons/TreeView_InterferingNode.png"},
								{IMAGE_TREEVIEW_INTERFERING_NODE_DISABLED, "icons/TreeView_InterferingNode_Disabled.png"},
								{IMAGE_TREEVIEW_INTERFERED_NODE, "icons/TreeView_InterferedNode.png"},
								{IMAGE_TREEVIEW_INTERFERED_NODE_DISABLED, "icons/TreeView_InterferedNode_Disabled.png"},

								{IMAGE_VIEW_CONTEXTMENU_FOLLOW_THREAD_REGION, "icons/ViewContextMenu_FollowThreadRegion.png"},
								{IMAGE_VIEW_CONTEXTMENU_FOLLOW_PARALLEL_REGIONS, "icons/ViewContextMenu_FollowParallelRegions.png"},
								{IMAGE_VIEW_CONTEXTMENU_FOLLOW_THREAD, "icons/ViewContextMenu_FollowThread.png"},
								{IMAGE_VIEW_CONTEXTMENU_CENTER_REGION, "icons/ViewContextMenu_CenterRegion.png"},

								{IMAGE_VIEW_CONTEXTMENU_FOLLOW_NODE, "icons/ViewContextMenu_FollowNode.png"},
								{IMAGE_VIEW_CONTEXTMENU_FOLLOW_INTERFERED_NODES, "icons/ViewContextMenu_FollowInterferedNodes.png"},
								{IMAGE_VIEW_CONTEXTMENU_CENTER_NODE, "icons/ViewContextMenu_CenterInterferedNode.png"},
								{IMAGE_VIEW_CONTEXTMENU_FOLLOW_INTERFERENCE, "icons/ViewContextMenu_FollowInterference.png"},

								{IMAGE_VIEW_TOOLBAR_FOLLOW_THREAD_REGION, "icons/ViewToolbar_FollowThreadRegion.png"},
								{IMAGE_VIEW_TOOLBAR_FOLLOW_PARALLEL_REGIONS, "icons/ViewToolbar_FollowParallelRegions.png"},
								{IMAGE_VIEW_TOOLBAR_FOLLOW_THREAD, "icons/ViewToolbar_FollowThread.png"},
								{IMAGE_VIEW_TOOLBAR_CENTER_REGION, "icons/ViewToolbar_CenterRegion.png"},

								{IMAGE_VIEW_TOOLBAR_FOLLOW_NODE, "icons/ViewToolbar_FollowNode.png"},
								{IMAGE_VIEW_TOOLBAR_FOLLOW_INTERFERED_NODES, "icons/ViewToolbar_FollowInterferedNodes.png"},
								{IMAGE_VIEW_TOOLBAR_CENTER_NODE, "icons/ViewToolbar_CenterInterferedNode.png"},

								{IMAGE_STATUSBAR, "icons/StatusBar.png"}
							};

		for (int i = 0; i < images.length; i++) {
			String im = images[i][1];
			URL url = null;

			try {
				url = new URL(Activator.getDefault().getBundle().getEntry("/"), im);

			} catch (MalformedURLException e) { }

			ImageDescriptor image = ImageDescriptor.createFromURL(url);
			ir.put(images[i][0], image);
		}
	}
}
