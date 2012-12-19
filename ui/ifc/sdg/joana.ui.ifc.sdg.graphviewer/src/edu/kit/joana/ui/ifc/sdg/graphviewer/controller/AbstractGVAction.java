/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)AbstractGVAction.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 13.12.2004 at 17:28:42
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import edu.kit.joana.ui.ifc.sdg.graphviewer.GraphViewer;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.util.GVUtilities;


import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public abstract class AbstractGVAction extends AbstractAction implements BundleConstants {
	private static final long serialVersionUID = 5890357776148592833L;
	private static final String ACCELERATORS_MAC_PROPERTIES = "Accelerators_mac.properties";
	private static final String ACCELERATORS_PROPERTIES = "Accelerators.properties";
	protected static final String DEFAULT_ICON = "Default.png";
	private static final Properties PROPERTIES = setProperties();

	static private Properties setProperties() {
		Properties p = new Properties();
		try {
			InputStream s = AbstractGVAction.class.getResourceAsStream("/" + ACCELERATORS_PROPERTIES);
			if (s == null)
				s = new FileInputStream(ACCELERATORS_PROPERTIES);
			p.load(s);
			if (GraphViewer.IS_MAC) {
				p = new Properties(p);
				s = AbstractGVAction.class.getResourceAsStream("/" + ACCELERATORS_MAC_PROPERTIES);
				if (s == null)
					s = new FileInputStream(ACCELERATORS_MAC_PROPERTIES);
				p.load(s);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return p;
	}

    /**
     * Constructs a new <code>AbstractGVAction</code> object.
     */
    protected AbstractGVAction(Resource name, String icon, Resource description) {
        this(name, icon, description, null);
    }

    /**
     * Constructs a new <code>AbstractGVAction</code> object.
     */
    protected AbstractGVAction(Resource name, Resource description) {
        this(name, DEFAULT_ICON, description, null);
    }

    /**
     * Constructs a new <code>AbstractGVAction</code> object.
     */
    protected AbstractGVAction(Resource name, Resource description, String accelerator) {
        this(name, DEFAULT_ICON, description, accelerator);
    }

    /**
     * Constructs a new <code>AbstractGVAction</code> object.
     */
    protected AbstractGVAction(Resource name, String icon, Resource description, String accelerator) {
        super();
        this.putValue(NAME, name);
        this.putValue(SMALL_ICON, GVUtilities.getIcon(icon));
        this.putValue(SHORT_DESCRIPTION, description);
        this.putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelerator));
    }

    /**
     * Constructs a new <code>AbstractGVAction</code> object.
     */
    protected AbstractGVAction(String name, String description) {
        this(name, DEFAULT_ICON, description, null);
    }

    /**
     * Constructs a new <code>AbstractGVAction</code> object.
     */
    protected AbstractGVAction(String name, String description, String accelerator) {
        this(name, DEFAULT_ICON, description, accelerator);
    }

    /**
     * Constructs a new <code>AbstractGVAction</code> object.
     */
    protected AbstractGVAction(String name, String icon, String description, String accelerator) {
        this(new Resource(ACTIONS_BUNDLE, name), icon, new Resource(
                ACTIONS_BUNDLE, description), accelerator == null? null : PROPERTIES.getProperty(accelerator));
    }
}
