/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)AttributeMapAdjustmentsListener.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 26.11.2004 at 16:25:10
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event;

import java.util.EventListener;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public interface AttributeMapAdjustmentsListener extends EventListener {

    public void attributeMapChanged(AttributeMapAdjustmentsEvent event);

}
