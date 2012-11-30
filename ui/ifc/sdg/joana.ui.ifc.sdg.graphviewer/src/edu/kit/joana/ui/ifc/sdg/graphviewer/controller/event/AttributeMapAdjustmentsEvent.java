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
/*
 * @(c)AttributeMapAdjustmentsEvent.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 26.11.2004 at 16:29:40
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event;

import java.util.EventObject;

import org.jgraph.graph.AttributeMap;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class AttributeMapAdjustmentsEvent extends EventObject {
	private static final long serialVersionUID = 1513228075977945684L;

	public AttributeMapAdjustmentsEvent(AttributeMap source) {
        super(source);
    }
}
