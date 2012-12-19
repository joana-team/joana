/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)GVPanel.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 06.11.2004 at 13:41:45
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view.component;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;


import java.awt.ComponentOrientation;
import java.awt.LayoutManager;

import javax.swing.JPanel;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class GVPanel extends JPanel implements LanguageListener {
	private static final long serialVersionUID = 916435745081361005L;

	protected Resource toolTip = null;

    protected Translator translator = null;

    public GVPanel(Translator translator) {
        this(translator, null, null);
    }

    /**
     * Constructs a new <code>GVPanel</code> object.
     */
    public GVPanel(Translator translator, LayoutManager manager) {
        this(translator, manager, null);
    }

    /**
     * Constructs a new <code>GVPanel</code> object.
     */
    public GVPanel(Translator translator, LayoutManager manager,
            Resource toolTip) {
        super();
        if (manager != null) {
            this.setLayout(manager);
        }
        this.translator = translator;
        this.toolTip = toolTip;
        this.translator.addLanguageListener(this);
    }

    public void setToolTipTextResource(Resource toolTip) {
        this.toolTip = toolTip;
        if (this.toolTip != null) {
            this.setToolTipText(this.translator.getString(toolTip));
        } else {
            this.setToolTipText(null);
        }
    }

    public Translator getTranslator() {
        return this.translator;
    }

    public void setTranslator(Translator translator) {
        this.translator.removeLanguageListener(this);
        this.translator = translator;
        this.translator.addLanguageListener(this);
    }

    /**
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener#languageChanged(edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent)
     */
    public void languageChanged(LanguageEvent event) {
        if (this.toolTip != null) {
            this.setToolTipText(this.translator.getString(this.toolTip));
        }
        this.setLocale(event.getLanguage());
        this.setComponentOrientation(ComponentOrientation.getOrientation(event
                .getLanguage()));
    }

}
