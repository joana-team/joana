/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.threadviewer.view.filter;



import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;
import edu.kit.joana.ui.ifc.sdg.threadviewer.model.SDGWrapper;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ViewInterferedNode;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ViewParallelRegion;




public class HideSourceCodeThreadRegionFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		boolean selected = true;

		if (element instanceof ThreadRegion) {
			ThreadRegion region = (ThreadRegion) element;
			selected = SDGWrapper.getInstance().isInSourceCode(region);
		} else if (element instanceof ViewParallelRegion) {
			ThreadRegion region = ((ViewParallelRegion) element).getItem();
			selected = SDGWrapper.getInstance().isInSourceCode(region);
		} else if (element instanceof SDGNode) {
			SDGNode node = (SDGNode) element;
			selected = SDGWrapper.getInstance().isInSourceCode(node);
		} else if (element instanceof ViewInterferedNode) {
			SDGNode node = ((ViewInterferedNode) element).getItem();
			selected = SDGWrapper.getInstance().isInSourceCode(node);
		}

		return selected;
	}

}
