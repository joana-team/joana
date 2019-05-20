/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu.util;

/**
 * Base class for all exceptions in this project.
 * 
 * Why extend the RuntimeException?
 * 
 * The main point is that the streaming API doesn't allow
 * throwing checked exceptions from lambdas used with it.
 * Therefore only unchecked exceptions can be thrown.
 * NildumuExceptions are also only thrown on fatal errors 
 * that the analysis cannot recover from.
 * Catching these exceptions is prohibited.
 */
public class NildumuException extends RuntimeException {

    public NildumuException(String message) {
        super(message);
    }
}
