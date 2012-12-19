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


public class SourcecodeThreadRegionSorter extends ViewerSorter {
	/*
	 * @see ViewerSorter#category(Object)
	 */
	/** Orders the items in such a way that books appear
	 * before moving boxes, which appear before board games. */
	public int category(Object element) {
		int cat = 0;

		if (element instanceof ThreadInstance) {
			cat = 0;
		} else if (element instanceof ThreadRegion) {
			ThreadRegion region = (ThreadRegion) element;

			if (SDGWrapper.getInstance().isInSourceCode(region)) {
				cat = 1;
			} else {
				cat = 2;
			}
		} else if (element instanceof ViewParallelRegion) {
			ThreadRegion region = ((ViewParallelRegion) element).getItem();

			if (SDGWrapper.getInstance().isInSourceCode(region)) {
				cat = 3;
			} else {
				cat = 4;
			}
		} else if (element instanceof SDGNode) {
			SDGNode node = (SDGNode) element;

			if (SDGWrapper.getInstance().isInSourceCode(node)) {
				cat = 5;
			} else {
				cat = 6;
			}
		} else if (element instanceof ViewInterferedNode) {
			SDGNode node = ((ViewInterferedNode) element).getItem();

			if (SDGWrapper.getInstance().isInSourceCode(node)) {
				cat = 7;
			} else {
				cat = 8;
			}
		}

		return cat;
	}

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
