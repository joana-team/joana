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
 * @(c)ZoomComboBox.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 06.11.2004 at 13:51:56
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVComboBox;


/**
 * So far not used.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class ZoomComboBox extends GVComboBox implements BundleConstants {
	private static final long serialVersionUID = -3432061688285416529L;
	private static final Resource[] SCALES = new Resource[] {
            new Resource(ACTIONS_BUNDLE, "zoom.25.label"),
            new Resource(ACTIONS_BUNDLE, "zoom.50.label"),
            new Resource(ACTIONS_BUNDLE, "zoom.100.label"),
            new Resource(ACTIONS_BUNDLE, "zoom.200.label"),
            new Resource(ACTIONS_BUNDLE, "zoom.400.label") };

    public ZoomComboBox(Translator translator) {
        super(translator, SCALES);
    }
}
