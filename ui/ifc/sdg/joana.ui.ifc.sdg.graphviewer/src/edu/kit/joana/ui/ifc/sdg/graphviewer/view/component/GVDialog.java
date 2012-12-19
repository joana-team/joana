/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)GVDialog.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 06.11.2004 at 13:20:24
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view.component;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;


import java.awt.ComponentOrientation;

import javax.swing.JDialog;

/**
 * The main class for creating a dialog window. You can use this class to create
 * a custom dialog, or invoke the many class methods in JOptionPane to create a
 * variety of standard dialogs.
 *
 * Contains only language support specific novelties.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class GVDialog extends JDialog implements LanguageListener {
	private static final long serialVersionUID = 2512016360610626890L;

	protected Resource title = null;

    protected Translator translator = null;

    /**
     * Constructs a new <code>GVDialog</code> object.
     */
    public GVDialog(Translator translator, GVDialog owner, Resource title,
            boolean modal) {
        super(owner, modal);
        this.translator = translator;
        this.title = title;
        this.translator.addLanguageListener(this);
    }

    /**
     * Constructs a new <code>GVDialog</code> object.
     */
    public GVDialog(Translator translator, GVFrame owner, Resource title,
            boolean modal) {
        super(owner, modal);
        this.translator = translator;
        this.title = title;
        this.translator.addLanguageListener(this);
    }

    /**
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener#languageChanged(edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent)
     */
    public void languageChanged(LanguageEvent event) {
        if (this.title != null) {
            this.setTitle(translator.getString(this.title));
        }
        this.setLocale(event.getLanguage());
        this.setComponentOrientation(ComponentOrientation.getOrientation(event
                .getLanguage()));
    }

    public Translator getTranslator() {
        return this.translator;
    }

    public void setTranslator(Translator translator) {
        this.translator.removeLanguageListener(this);
        this.translator = translator;
        this.translator.addLanguageListener(this);
    }

}
