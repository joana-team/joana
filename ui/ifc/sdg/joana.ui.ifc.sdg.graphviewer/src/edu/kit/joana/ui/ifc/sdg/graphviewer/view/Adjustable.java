/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)Adjustable.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 02.11.2004 at 15:17:25
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVPanel;


/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public interface Adjustable {

    /**
     * @return a string to be displayed in some component
     */
    public Resource getKeyResource();

    /**
     * @return a dialog that offers some adjustment possibilities
     */
    public GVPanel getAdjustmentDialog();

    /**
     * Reacts when the user commits the adjustments he made by clicking OK.
     *
     * @param valuesChanged
     *            <code>true</code> if anything changed
     */
    public void adjustmentPerformed(boolean valuesChanged);

}
