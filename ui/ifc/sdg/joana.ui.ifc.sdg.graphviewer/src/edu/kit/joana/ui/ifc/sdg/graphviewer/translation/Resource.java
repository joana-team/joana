/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)Resource.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 28.11.2004 at 14:18:10
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.translation;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class Resource {

    protected String bundle = null;

    protected String key = null;

    protected String opt = null;

    public Resource(String bundle, String key) {
        this.bundle = bundle;
        this.key = key;
    }

    public Resource(String bundle, String key, String opt) {
        this.bundle = bundle;
        this.key = key;
        this.opt = opt;
    }

    public String getBundle() {
        return this.bundle;
    }

    public String getKey() {
        return this.key;
    }

    public boolean hasOpt() {
    	return this.opt != null;
    }

    public String getOpt() {
        return this.opt;
    }

}
