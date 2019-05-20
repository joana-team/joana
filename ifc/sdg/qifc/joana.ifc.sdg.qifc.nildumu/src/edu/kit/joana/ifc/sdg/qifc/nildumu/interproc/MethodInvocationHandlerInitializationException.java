/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu.interproc;

import edu.kit.joana.ifc.sdg.qifc.nildumu.util.NildumuException;

public class MethodInvocationHandlerInitializationException extends NildumuException {

    public MethodInvocationHandlerInitializationException(String message) {
        super("Error initializing the method invocation handler: " + message);
    }
}