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
package edu.kit.joana.ui.ifc.sdg.threadviewer.view.sorter;



import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ui.ifc.sdg.threadviewer.model.SDGWrapper;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ViewInterferedNode;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ViewParallelRegion;


public class AlphabeticalThreadRegionSorter extends ViewerSorter {
	/*
	 * @see ViewerSorter#category(Object)
	 */
	public int category(Object element) {
		int cat = 0;
		if (element instanceof ThreadInstance) {
			cat = 0;
		} else if (element instanceof ThreadRegion) {
			cat = 1;
		} else if (element instanceof ViewParallelRegion) {
			cat = 2;
		} else if (element instanceof SDGNode) {
			cat = 3;
		} else if (element instanceof ViewInterferedNode) {
			cat = 4;
		}

		return cat;
	}

	// Sort alphabetical 1st, then start row number
	@SuppressWarnings("deprecation")
	public int compare(Viewer viewer, Object e1, Object e2) {
		int cat1 = category(e1);
		int cat2 = category(e2);

		if (cat1 != cat2) {
			return cat1 - cat2;
		}

		if (e1 instanceof ThreadInstance) {
			ThreadInstance thread1 = (ThreadInstance) e1;
			ThreadInstance thread2 = (ThreadInstance) e2;
			String s1 = SDGWrapper.getInstance().getLabel(thread1);
			String s2 = SDGWrapper.getInstance().getLabel(thread2);
			return collator.compare(s1, s2);
		} else if (e1 instanceof ThreadRegion) {
			ThreadRegion region1 = (ThreadRegion) e1;
			ThreadRegion region2 = (ThreadRegion) e2;
			String s1 = SDGWrapper.getInstance().getLabel(region1);
			String s2 = SDGWrapper.getInstance().getLabel(region2);
			return collator.compare(s1, s2);
		} else if (e1 instanceof ViewParallelRegion) {
			ViewParallelRegion region1 = (ViewParallelRegion) e1;
			ViewParallelRegion region2 = (ViewParallelRegion) e2;
			String s1 = SDGWrapper.getInstance().getLabel(region1);
			String s2 = SDGWrapper.getInstance().getLabel(region2);
			return collator.compare(s1, s2);
		} else if (e1 instanceof SDGNode) {
			SDGNode node1 = (SDGNode) e1;
			SDGNode node2 = (SDGNode) e2;
			String s1 = SDGWrapper.getInstance().getLabel(node1);
			String s2 = SDGWrapper.getInstance().getLabel(node2);
			return collator.compare(s1, s2);
		} else if (e1 instanceof ViewInterferedNode) {
			ViewInterferedNode node1 = (ViewInterferedNode) e1;
			ViewInterferedNode node2 = (ViewInterferedNode) e2;
			String s1 = SDGWrapper.getInstance().getLabel(node1);
			String s2 = SDGWrapper.getInstance().getLabel(node2);
			return collator.compare(s1, s2);
		}

		return 0;
	}
}
