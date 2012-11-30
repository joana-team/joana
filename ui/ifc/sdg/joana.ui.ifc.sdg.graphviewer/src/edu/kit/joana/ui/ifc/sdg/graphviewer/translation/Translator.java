/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)Translator.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 28.11.2004 at 14:11:32
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.translation;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.LanguageListener;

import java.util.Locale;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public interface Translator {

    public Locale getLanguage();

    public String getString(Resource resource);

    public void addLanguageListener(LanguageListener listener);

    public void removeLanguageListener(LanguageListener listener);

}
