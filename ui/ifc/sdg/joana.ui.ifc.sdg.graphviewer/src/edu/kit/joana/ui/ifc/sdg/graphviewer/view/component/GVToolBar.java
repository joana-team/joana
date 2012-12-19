/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)GVToolBar.java
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

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JToolBar;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class GVToolBar extends JToolBar implements LanguageListener {
	private static final long serialVersionUID = -7885269857559600037L;

	protected Resource name = null;

    protected Translator translator = null;

    /**
     * Constructs a new <code>GVToolBar</code> object.
     */
    public GVToolBar(Translator translator, Resource name) {
        super();
        this.translator = translator;
        this.name = name;
        this.setBorderPainted(true);
        this.translator.addLanguageListener(this);
    }

    @Override
	public JButton add(Action action) {
        GVButton button = new GVButton(this.translator, action);
        button.setTextResource(null);
        this.add(button);
        return button;
    }

    public void setNameResource(Resource name) {
        this.name = name;
        if (this.name != null) {
            this.setName(this.translator.getString(name));
        } else {
            this.setName(null);
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
        if (this.name != null) {
            this.setName(this.translator.getString(this.name));
        }
        this.setLocale(event.getLanguage());
        this.setComponentOrientation(ComponentOrientation.getOrientation(event
                .getLanguage()));
    }

}
