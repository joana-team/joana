/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)GVOptionPane.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 16.11.2004 at 11:32:10
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view.component;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;


import java.awt.ComponentOrientation;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class GVOptionPane extends JComponent implements BundleConstants, LanguageListener {
	private static final long serialVersionUID = -6305373061021210099L;

	protected GVFrame owner = null;

    protected Translator translator = null;

    /**
     * Constructs a new <code>GVOptionPane</code> object.
     */
    public GVOptionPane(GVFrame owner, Translator translator) {
        this.owner = owner;
        this.translator = translator;
        this.translator.addLanguageListener(this);
    }

    /**
     * Constructs a new <code>GVOptionPane</code> object.
     */
    public GVOptionPane(GVFrame owner) {
        this(owner, owner.getTranslator());
    }

    public void showInformationDialog(Resource message) {
        String[] options = new String[] { this.translator
                .getString(new Resource(MAIN_FRAME_BUNDLE, "ok.label")) };
        JOptionPane.showOptionDialog(this.owner, this.translator
                .getString(message) + (message.hasOpt() ? message.getOpt() : ""),
                this.translator.getString(new Resource(
                MAIN_FRAME_BUNDLE, "information.dialog.title")),
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);
    }

    public void showErrorDialog(Resource message) {
        String[] options = new String[] { this.translator
                .getString(new Resource(MAIN_FRAME_BUNDLE, "ok.label")) };
        JOptionPane.showOptionDialog(this.owner, this.translator
                .getString(message), this.translator.getString(new Resource(
                MAIN_FRAME_BUNDLE, "error.dialog.title")),
                JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null,
                options, options[0]);
    }

    /**
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener#languageChanged(edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent)
     */
    public void languageChanged(LanguageEvent event) {
        this.setLocale(translator.getLanguage());
        this.setComponentOrientation(ComponentOrientation
                .getOrientation(translator.getLanguage()));
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
