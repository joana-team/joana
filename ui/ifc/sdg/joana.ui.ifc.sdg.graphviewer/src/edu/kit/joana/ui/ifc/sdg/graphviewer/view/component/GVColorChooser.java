/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)GVColorChooser.java
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

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JColorChooser;
import javax.swing.JDialog;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;

/**
 * JColorChooser provides a pane of controls designed to allow a user to
 * manipulate and select a color.
 *
 * Contains only language support specific novelties.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class GVColorChooser extends JColorChooser implements LanguageListener {
	private static final long serialVersionUID = -5161102043938245566L;

	protected Resource title = null;

    protected Translator translator = null;

    /**
     * Constructs a new <code>GVColorChooser</code> object.
     */
    public GVColorChooser(Translator translator, Resource title) {
        super();
        this.translator = translator;
        this.title = title;
        this.translator.addLanguageListener(this);
    }

    public Color showDialog(GVPanel owner, Color col) {

        this.setColor(col);

        ColorTracker ok = new ColorTracker(this);
        JDialog dialog = createDialog(owner, translator.getString(title), true,
                this, ok, null);
        dialog.setLocale(this.getLocale());

        // blocks until user brings dialog down...
        dialog.setVisible(true);

        return ok.getColor();

    }

    /**
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener#languageChanged(edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent)
     * @param event
     */
    public void languageChanged(LanguageEvent event) {
        this.setLocale(event.getLanguage());
        this.setComponentOrientation(ComponentOrientation.getOrientation(event
                .getLanguage()));
        this.updateUI();
    }

    public Translator getTranslator() {
        return this.translator;
    }

    public void setTranslator(Translator translator) {
        this.translator.removeLanguageListener(this);
        this.translator = translator;
        this.translator.addLanguageListener(this);
    }

    protected class ColorTracker implements ActionListener {

        private GVColorChooser chooser;

        private Color color;

        public ColorTracker(GVColorChooser chooser) {
            this.chooser = chooser;
        }

        public void actionPerformed(ActionEvent event) {
            this.color = this.chooser.getColor();
        }

        public Color getColor() {
            return this.color;
        }

    }
}
