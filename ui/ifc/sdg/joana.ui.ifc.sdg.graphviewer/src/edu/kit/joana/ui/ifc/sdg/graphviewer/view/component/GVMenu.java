/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)GVMenu.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 28.10.2004 at 20:03:45
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view.component;

import java.awt.ComponentOrientation;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class GVMenu extends JMenu implements LanguageListener {
	private static final long serialVersionUID = 742957694766926995L;

	protected Resource text = null;

    protected Resource toolTip = null;

    protected Translator translator = null;

    /**
     * Constructs a new <code>GVMenu</code> object.
     */
    public GVMenu(Translator translator, Resource text) {
        this(translator, text, null);
    }

    /**
     * Constructs a new <code>GVMenu</code> object.
     */
    public GVMenu(Translator translator, Resource text, Resource toolTip) {
        super();
        this.translator = translator;
        this.text = text;
        this.toolTip = toolTip;
        this.translator.addLanguageListener(this);
    }

    @Override
	public JMenuItem add(Action action) {
        return super.add(new GVMenuItem(this.translator, action));
    }

    public void setTextResource(Resource text) {
        this.text = text;
        if (this.text == null) {
            this.setText(null);
        } else {
            this.setText(this.translator.getString(text));
        }
    }

    public void setToolTipResource(Resource toolTip) {
        this.toolTip = toolTip;
        if (this.toolTip == null) {
            this.setToolTipText(null);
        } else {
            this.setToolTipText(this.translator.getString(toolTip));
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
     * @param event
     */
    public void languageChanged(LanguageEvent event) {
        if (this.text != null) {
            this.setText(this.translator.getString(this.text));
        }
        if (this.toolTip != null) {
            this.setToolTipText(this.translator.getString(this.toolTip));
        }
        this.setLocale(event.getLanguage());
        this.setComponentOrientation(ComponentOrientation.getOrientation(event
                .getLanguage()));
    }

}
