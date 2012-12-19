/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view.component;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;


import javax.swing.JRadioButton;

/**
 * Contains only language support specific novelties.
 */
public class GVRadioButton extends JRadioButton implements LanguageListener {
	private static final long serialVersionUID = 6447981849361997420L;

	protected Resource text = null;

    protected Translator translator = null;


    /**
     * Constructs a new <code>GVRadioButton</code> object.
     */
    public GVRadioButton(Translator translator) {
        this(translator, (Resource) null);
    }

    /**
     * Constructs a new <code>GVRadioButton</code> object.
     */
    public GVRadioButton(Translator translator, Resource text) {
        super();
        this.translator = translator;
        this.text = text;
        this.translator.addLanguageListener(this);
    }


    public void setTextResource(Resource text) {
        this.text = text;
        if (this.text != null) {
            // Sets the button's text.
            this.setText(this.translator.getString(text));
        } else {
            this.setText(null);
        }
    }


    /**
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener#languageChanged(edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent)
     * @param event
     */
    public void languageChanged(LanguageEvent event) {
        if (this.text != null) {
            this.setText(this.translator.getString(this.text));
        }

        this.setLocale(event.getLanguage());
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
