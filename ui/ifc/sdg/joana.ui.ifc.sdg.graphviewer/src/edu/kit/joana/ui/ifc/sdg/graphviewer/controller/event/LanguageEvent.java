/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)LanguageEvent.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 02.11.2004 at 14:58:41
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event;

import java.util.EventObject;
import java.util.Locale;

import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;


/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class LanguageEvent extends EventObject {
	private static final long serialVersionUID = -6341928096266110285L;
	protected Locale language = null;

    public LanguageEvent(Translator source, Locale language) {
        super(source);
        this.language = language;
    }

    public Locale getLanguage() {
        return this.language;
    }
}
