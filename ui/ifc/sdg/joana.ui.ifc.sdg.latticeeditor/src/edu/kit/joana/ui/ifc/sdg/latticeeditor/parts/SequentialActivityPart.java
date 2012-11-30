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
/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package edu.kit.joana.ui.ifc.sdg.latticeeditor.parts;

import java.util.List;
import java.util.Map;


import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.graph.CompoundDirectedGraph;
import org.eclipse.draw2d.graph.Edge;
import org.eclipse.draw2d.graph.Node;

import org.eclipse.gef.EditPart;

import edu.kit.joana.ui.ifc.sdg.latticeeditor.figures.SequentialActivityFigure;

/**
 * @author hudsonr Created on Jul 18, 2003
 */
public class SequentialActivityPart extends StructuredActivityPart {

	/**
	 * @see edu.kit.joana.ifc.sdg.latticeeditor.parts.StructuredActivityPart#createFigure()
	 */
	protected IFigure createFigure() {
		return new SequentialActivityFigure();
	}

	/**
	 * @see ActivityPart#contributeEdgesToGraph(org.eclipse.graph.CompoundDirectedGraph,
	 *      java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	public void contributeEdgesToGraph(CompoundDirectedGraph graph, Map map) {
		super.contributeEdgesToGraph(graph, map);
		Node node, prev = null;
		EditPart a;
		List members = getChildren();
		for (int n = 0; n < members.size(); n++) {
			a = (EditPart) members.get(n);
			node = (Node) map.get(a);
			if (prev != null) {
				Edge e = new Edge(prev, node);
				e.weight = 50;
				graph.edges.add(e);
			}
			prev = node;
		}
	}

	/**
	 * @see edu.kit.joana.ifc.sdg.latticeeditor.parts.StructuredActivityPart#getAnchorOffset()
	 */
	int getAnchorOffset() {
		return 15;
	}

}
