/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph;

import org.jgrapht.EdgeFactory;

/**
 * The factory for creating Vertices and Edges in a <tt>DirectedGraphImpl</tt> class.
 *
 * @author Christian Hammer
 */

public class SDGFactory implements EdgeFactory<SDGNode, SDGEdge> {

    public SDGEdge createEdge( SDGNode v1, SDGNode v2 ) {
        return  SDGEdge.Kind.HELP.newEdge( v1, v2);
    }
}
