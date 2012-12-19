/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)GVUtilities.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 19.11.2004 at 17:39:14
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.util;

import java.io.File;
import java.net.URL;

import javax.swing.ImageIcon;

/**
 * This is a utility class.
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @author Siegfried Weber
 * @version 1.1
 */
public final class GVUtilities implements PathConstants {

    /**
     * Returns the icon with the specified name.
     * @param iconName the name of the icon
     * @return an icon
     */
    public static ImageIcon getIcon(String iconName) {
        ImageIcon icon = null;
        // use this for jar distribution
        URL url = GVUtilities.class.getResource("/" + ICON_PATH + iconName);
        if (url != null)
        	icon = new ImageIcon(url);
        else
        	icon = new ImageIcon(ICON_PATH + iconName);
        return icon;
    }

    /**
     * Returns the extension of a file name.
     * @param file a file
     * @return the extension
     */
    public static String getFileExtension(File file) {
        String extension = null;
        String name = file.getName();
        int i = name.lastIndexOf('.');
        if ((i > 0) && (i < name.length() - 1)) {
            extension = name.substring(i + 1).toLowerCase();
        }
        return extension;
    }

}
