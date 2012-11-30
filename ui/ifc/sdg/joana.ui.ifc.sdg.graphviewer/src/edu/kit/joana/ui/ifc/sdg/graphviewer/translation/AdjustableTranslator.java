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
 * @(c)AdjustableTranslator.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 15.12.2004 at 22:49:34
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.translation;

import edu.kit.joana.ui.ifc.sdg.graphviewer.view.Adjustable;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.LanguageAdjustmentsPanel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVPanel;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class AdjustableTranslator extends DefaultTranslator implements
        Adjustable {

    protected static AdjustableTranslator instance = null;

    private LanguageAdjustmentsPanel adjustmentsPanel = null;

    protected AdjustableTranslator() {
        super();
    }

    public static AdjustableTranslator getInstance() {
        if (instance == null) {
            instance = new AdjustableTranslator();
        }
        return instance;
    }

    /**
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.view.Adjustable#adjustmentPerformed(boolean)
     */
    public void adjustmentPerformed(boolean valuesChanged) {
        if (valuesChanged) {
            if (this.adjustmentsPanel != null) {
                this.setLanguage(this.adjustmentsPanel.getLanguage());
            }
        } else {
            if (this.adjustmentsPanel != null) {
                this.adjustmentsPanel.storeValues(this.language);
            }
        }
    }

    /**
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.view.Adjustable#getAdjustmentDialog()
     * @return
     */
    public GVPanel getAdjustmentDialog() {
        if (this.adjustmentsPanel == null) {
            this.adjustmentsPanel = new LanguageAdjustmentsPanel();
        }
        this.adjustmentsPanel.storeValues(this.language);
        return this.adjustmentsPanel;
    }

    /**
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.view.Adjustable#getKeyResource()
     * @return
     */
    public Resource getKeyResource() {
        return new Resource(LANGUAGE_ADJUSTMENTS_BUNDLE, "language.label");
    }

}
