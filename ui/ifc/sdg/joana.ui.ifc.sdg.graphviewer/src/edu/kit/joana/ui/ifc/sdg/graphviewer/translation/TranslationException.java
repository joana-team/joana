/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)TranslationException.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 28.10.2004 at 17:42:36
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.translation;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class TranslationException extends RuntimeException {
	private static final long serialVersionUID = -6405434675749164349L;

	/**
     * Constructs a new <code>TranslationException</code> object.
     */
    public TranslationException() {
        super();
    }

    /**
     * Constructs a new <code>TranslationException</code> object.
     *
     * @param message
     *            the exception message.
     */
    public TranslationException(String message) {
        super(message);
    }
}
