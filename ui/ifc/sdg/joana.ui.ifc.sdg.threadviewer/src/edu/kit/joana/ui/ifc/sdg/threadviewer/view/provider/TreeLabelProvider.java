/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.threadviewer.view.provider;


import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ui.ifc.sdg.threadviewer.model.SDGWrapper;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.Activator;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ViewCategory;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ViewInterferedNode;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ViewInterferenceCategory;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ViewParallelCategory;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ViewParallelRegion;


public class TreeLabelProvider extends LabelProvider {
	@Override
	public String getText(Object obj) {
		if (obj instanceof ThreadInstance) {
			ThreadInstance thread = (ThreadInstance) obj;
			return SDGWrapper.getInstance().getLabel(thread);
		} else if (obj instanceof ThreadRegion) {
			ThreadRegion region = (ThreadRegion) obj;
			return SDGWrapper.getInstance().getLabel(region);
		} else if (obj instanceof ViewCategory) {
			ViewCategory category = (ViewCategory) obj;
			return category.getLabel();
		} else if (obj instanceof ViewParallelRegion) {
			ViewParallelRegion item = (ViewParallelRegion) obj;
			return SDGWrapper.getInstance().getLabel(item);
		} else if (obj instanceof SDGNode) {
			SDGNode node = (SDGNode) obj;
			return SDGWrapper.getInstance().getLabel(node);
		} else if (obj instanceof ViewInterferedNode) {
			ViewInterferedNode node = (ViewInterferedNode) obj;
			return SDGWrapper.getInstance().getLabel(node);
		}

		return "";
	}

	public Image getColumnImage(Object obj, int index) {
		return getImage(obj);
	}

	@Override
	public Image getImage(Object obj) {
		if (obj instanceof ThreadInstance) {
			return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_TREEVIEW_THREAD);
		} else if (obj instanceof ThreadRegion) {
			ThreadRegion region = (ThreadRegion) obj;

			if (SDGWrapper.getInstance().isInSourceCode(region)) {
				return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_TREEVIEW_THREADREGION);
			} else {
				return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_TREEVIEW_THREADREGION_DISABLED);
			}
		} else if (obj instanceof ViewParallelRegion) {
			ThreadRegion region = ((ViewParallelRegion) obj).getItem();

			if (SDGWrapper.getInstance().isInSourceCode(region)) {
				return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_TREEVIEW_THREADREGION);
			} else {
				return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_TREEVIEW_THREADREGION_DISABLED);
			}
		} else if (obj instanceof ViewParallelCategory) {
			return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_TREEVIEW_PARALLEL_CATEGORY);
		} else if (obj instanceof ViewInterferenceCategory) {
			return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_TREEVIEW_INTERFERENCE_CATEGORY);
		} else if (obj instanceof SDGNode) {
			SDGNode node = (SDGNode) obj;

			if (SDGWrapper.getInstance().isInSourceCode(node)) {
				return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_TREEVIEW_INTERFERING_NODE);
			} else {
				return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_TREEVIEW_INTERFERING_NODE_DISABLED);
			}
		} else if (obj instanceof ViewInterferedNode) {
			SDGNode node = ((ViewInterferedNode) obj).getItem();

			if (SDGWrapper.getInstance().isInSourceCode(node)) {
				return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_TREEVIEW_INTERFERED_NODE);
			} else {
				return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_TREEVIEW_INTERFERED_NODE_DISABLED);
			}
		}

		return PlatformUI.getWorkbench().
				getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
	}
}
