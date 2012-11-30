/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*****************************************************************************
 *                                                                           *
 *   Exception for Invalid Chopping Criteria                                 *
 *                                                                           *
 *   This file is part of the chopper package of the sdg library.            *
 *   The used chopping algorithms are desribed in Jens Krinke's PhD thesis   *
 *   "Advanced Slicing of Sequential and Concurrent Programs".               *
 *                                                                           *
 *   authors:                                                                *
 *   Bernd Nuernberger, nuerberg@fmi.uni-passau.de                           *
 *                                                                           *
 *****************************************************************************/

package edu.kit.joana.ifc.sdg.graph.chopper;

/**
 * Indicates that a chopping criterion is invalid.
 *
 * For example, a same-level chopper requires that the nodes of the criterion stem from the same procedure.
 *
 * @author Bernd Nuernberger
 */
public class InvalidCriterionException extends RuntimeException {
    private static final long serialVersionUID = 8799451095831121558L;

    /**
     * Constructs an <code>InvalidCriterionException</code> with no specified
     * detail message.
     */
    public InvalidCriterionException() {
        super();
    }

    /**
     * Constructs an <code>InvalidCriterionException</code> with a specified
     * detail message.
     * @param message The detail message.
     */
    public InvalidCriterionException(String message) {
        super(message);
    }
}
