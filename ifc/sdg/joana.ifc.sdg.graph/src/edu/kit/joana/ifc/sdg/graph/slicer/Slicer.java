/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * -- Created on October 14, 2005
 *
 * @author  Dennis Giffhorn
 */
public interface Slicer {

    public void setGraph(SDG graph);

    public Collection<SDGNode> slice(Collection<SDGNode> criteria);

    public Collection<SDGNode> slice(SDGNode criterion);
}
