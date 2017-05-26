/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.model;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.EdgeViewSettings;


public abstract class Graph {
	public static final Set<SDGEdge.Kind> alwaysHide = EnumSet.of(SDGEdge.Kind.HELP);
	public static final Set<SDGEdge.Kind> showCF = EnumSet.of(SDGEdge.Kind.CONTROL_FLOW);
	public static final Set<SDGEdge.Kind> showCD = EnumSet.of(SDGEdge.Kind.CONTROL_DEP_CALL,
															SDGEdge.Kind.CONTROL_DEP_COND,
															SDGEdge.Kind.CONTROL_DEP_EXPR,
															SDGEdge.Kind.CONTROL_DEP_UNCOND,
															SDGEdge.Kind.SUMMARY,
															SDGEdge.Kind.SUMMARY_NO_ALIAS);
//															SDGEdge.Kind.HELP);
	public static final Set<SDGEdge.Kind> showDD = EnumSet.of(SDGEdge.Kind.DATA_DEP,
															SDGEdge.Kind.DATA_DEP_EXPR_REFERENCE,
															SDGEdge.Kind.DATA_DEP_EXPR_VALUE,
															SDGEdge.Kind.SUMMARY,
															SDGEdge.Kind.SUMMARY_NO_ALIAS,
															SDGEdge.Kind.SUMMARY_DATA);
	public static final Set<SDGEdge.Kind> showDH = EnumSet.of(SDGEdge.Kind.DATA_HEAP, SDGEdge.Kind.DATA_ALIAS);
	public static final Set<SDGEdge.Kind> showPS = EnumSet.of(SDGEdge.Kind.PARAMETER_STRUCTURE, SDGEdge.Kind.PARAMETER_EQUIVALENCE);

	public static final Set<SDGEdge.Kind> showIF = EnumSet.of(SDGEdge.Kind.INTERFERENCE,
			SDGEdge.Kind.INTERFERENCE_WRITE,
			SDGEdge.Kind.SYNCHRONIZATION,
			SDGEdge.Kind.JOIN,
			SDGEdge.Kind.JOIN_OUT);

	private LinkedList<GraphObserver> views;
	// no control flow and no parameter structure as default view
	private final EdgeViewSettings myb = new EdgeViewSettings(true, false, true, true, false, true);


	public abstract SDG getSDG();
	public abstract SDG getCompleteSDG();
	public abstract String getName();


	public void attach(GraphObserver v) {
		views = new LinkedList<GraphObserver>();
		views.add(v);
	}

	public void detach(GraphObserver v) {
		views.remove(v);
	}

	public void close() {
		for (GraphObserver v : views) {
			v.close();
		}
		views.clear();
	}

	public EdgeViewSettings getEdgeViewSettings() {
		return myb;
	}

	public Set<String> getEdgesToHideAsStrings() {
		Set<String> toHideStr = new HashSet<String>();
		for (SDGEdge.Kind kind : alwaysHide) {
			toHideStr.add(kind.toString());
		}

		if (!myb.isShowCF()) {
			for (SDGEdge.Kind kind : showCF) {
				toHideStr.add(kind.toString());
			}
		}

		if (!myb.isShowCD()) {
			for (SDGEdge.Kind kind : showCD) {
				toHideStr.add(kind.toString());
			}
		}

		if (!myb.isShowDD()) {
			for (SDGEdge.Kind kind : showDD) {
				toHideStr.add(kind.toString());
			}
		}

		if (!myb.isShowDH()) {
			for (SDGEdge.Kind kind : showDH) {
				toHideStr.add(kind.toString());
			}
		}

		if (!myb.isShowPS()) {
			for (SDGEdge.Kind kind : showPS) {
				toHideStr.add(kind.toString());
			}
		}

		if (!myb.isShowIF()) {
			for (SDGEdge.Kind kind : showIF) {
				toHideStr.add(kind.toString());
			}
		}

		return toHideStr;
	}

	public void changed() {
		for (GraphObserver v : views) {
			v.refresh();
		}
	}
}
