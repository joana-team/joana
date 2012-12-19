/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)GVPopupMenu.java
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

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;


import java.awt.ComponentOrientation;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

/**
 * A JPopupMenu is used for the menu that appears when the user selects an item
 * on the menu bar. It is also used for "pull-right" menu that appears when the
 * user selects a menu item that activates it. A JPopupMenu can also be used
 * anywhere else you want a menu to appear. For example, when the user
 * right-clicks in a specified area.
 *
 * Contains only language support specific novelties.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class GVPopupMenu extends JPopupMenu implements LanguageListener {
	private static final long serialVersionUID = -7101664302242199323L;

	protected Resource toolTip = null;

    protected Translator translator = null;

    /**
     * Constructs a new <code>GVPopupMenu</code> object.
     */
    public GVPopupMenu(Translator translator) {
        this(translator, null);
    }

    /**
     * Constructs a new <code>GVPopupMenu</code> object.
     */
    public GVPopupMenu(Translator translator, Resource toolTip) {
        super();
        this.translator = translator;
        this.toolTip = toolTip;
        this.translator.addLanguageListener(this);
    }

    @Override
	public JMenuItem add(Action action) {
        return super.add(new GVMenuItem(this.translator, action));
    }

    public void setToolTipResource(Resource toolTip) {
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
     * @param event
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
