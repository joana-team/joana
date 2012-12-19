/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event;

import java.util.EventObject;
import java.util.Properties;

public class RefreshViewEvent extends EventObject {
	private static final long serialVersionUID = 570847067632694808L;

	public RefreshViewEvent(Properties source) {
        super(source);
    }

}

