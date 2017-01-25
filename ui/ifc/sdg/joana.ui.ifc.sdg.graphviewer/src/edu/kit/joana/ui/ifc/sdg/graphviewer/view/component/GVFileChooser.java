/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)GVFileChooser.java
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

import java.awt.ComponentOrientation;

import javax.swing.JFileChooser;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;
import edu.kit.joana.ui.ifc.sdg.graphviewer.util.Debug;

/**
 * Contains only language support specific novelties.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class GVFileChooser extends JFileChooser implements LanguageListener {
	private static final long serialVersionUID = -9182790267204570013L;

	protected Resource title = null;

    protected Translator translator = null;

    /**
     * Constructs a new <code>GVFileChooser</code> object.
     */
    public GVFileChooser(Translator translator, Resource title) {
        super();
        this.translator = translator;
        this.title = title;
        this.translator.addLanguageListener(this);
    }

    /**
     * @see edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener#languageChanged(edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageEvent)
     * @param event
     */
    public void languageChanged(LanguageEvent event) {
        Debug.print("GVFileChooser.languageChanged(), Sprache: "
                + event.getLanguage().toString());
        if (this.title != null) {
            this.setDialogTitle(translator.getString(this.title));
        }
        this.setLocale(event.getLanguage());
        this.setComponentOrientation(ComponentOrientation.getOrientation(event
                .getLanguage()));
        this.getUI().uninstallUI(this);
        this.getUI().installUI(this);
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
