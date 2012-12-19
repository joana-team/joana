/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)PDGFilter.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 14.11.2004 at 10:45:12
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller;

import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;
import edu.kit.joana.ui.ifc.sdg.graphviewer.util.GVUtilities;


import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class PDGFilter extends FileFilter {

    private static final String[] EXTENSIONS = new String[] { "pdg" };

    protected Translator translator = null;

    public PDGFilter(Translator translator) {
        this.translator = translator;
    }

    /**
     * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
     */
    @Override
	public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        } else {
            for (int i = 0; i < EXTENSIONS.length; i++) {
                String extension = GVUtilities.getFileExtension(file);
                if (extension != null && extension.equals(EXTENSIONS[i])) {
                    return true;
                }
            }
            return false;
        }

    }

    /**
     * @see javax.swing.filechooser.FileFilter#getDescription()
     */
    @Override
	public String getDescription() {
        String description = this.translator.getString(new Resource(
                BundleConstants.ACTIONS_BUNDLE, "pdg.filter"));
        if (EXTENSIONS.length > 0) {
            description += " (*." + EXTENSIONS[0];
            for (int i = 1; i < EXTENSIONS.length; i++) {
                description += ", *." + EXTENSIONS[i];
            }
            description += ")";
        }
        return description;
    }

}
