/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)GVMenuItem.java
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
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

/**
 * An implementation of an item in a menu. A menu item is essentially a button
 * sitting in a list. When the user selects the "button", the action associated
 * with the menu item is performed. A JMenuItem contained in a JPopupMenu
 * performs exactly that function.
 *
 * Contains only language support specific novelties.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class GVMenuItem extends JMenuItem implements LanguageListener {
	private static final long serialVersionUID = 2267474834519383632L;

	protected Resource text = null;

    protected Resource toolTip = null;

    protected Translator translator = null;

    /**
     * Constructs a new <code>GVMenuItem</code> object.
     */
    public GVMenuItem(Translator translator, Action action) {
        super();
        this.translator = translator;
        this.setAction(action);
        this.translator.addLanguageListener(this);
    }

    // Factory method which sets the ActionEvent source's properties according
    // to values from the Action instance.
    @Override
	protected void configurePropertiesFromAction(Action action) {
        String[] types = new String[] { Action.MNEMONIC_KEY, Action.NAME,
                Action.SHORT_DESCRIPTION, Action.SMALL_ICON,
                Action.ACTION_COMMAND_KEY, Action.ACCELERATOR_KEY, "enabled" };
        for (int i = 0; i < types.length; i++) {
            String type = types[i];
            if (type.equals(Action.MNEMONIC_KEY)) {
                Integer n = (action == null) ? null : (Integer) action
                        .getValue(type);
                this.setMnemonic(n == null ? '\0' : n.intValue());
            } else if (type.equals(Action.NAME)) {
                Boolean hide = (Boolean) this
                        .getClientProperty("hideActionText");
                this
                        .setTextResource(action != null && hide != Boolean.TRUE ? (Resource) action
                                .getValue(Action.NAME)
                                : null);
            } else if (type.equals(Action.SHORT_DESCRIPTION)) {
                this.setToolTipResource(action != null ? (Resource) action
                        .getValue(type) : null);
            } else if (type.equals(Action.SMALL_ICON)) {
                this.setIcon(action != null ? (Icon) action.getValue(type)
                        : null);
            } else if (type.equals(Action.ACTION_COMMAND_KEY)) {
                this.setActionCommand(action != null ? (String) action
                        .getValue(type) : null);
            } else if (type.equals(Action.ACCELERATOR_KEY)) {
                this.setAccelerator(action != null ? (KeyStroke) action
                        .getValue(type) : null);
            } else if (type.equals("enabled")) {
                this.setEnabled(action != null ? action.isEnabled() : true);
            }
        }
    }

    public void setTextResource(Resource text) {
        this.text = text;
        if (this.text != null) {
            this.setText(this.translator.getString(text));
        } else {
            this.setText(null);
        }
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
