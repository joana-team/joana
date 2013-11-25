/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * Represents a part of a java method or the java method itself.
 *
 * @author Martin Mohr
 *
 */
public interface SDGProgramPart {
	<R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data);
	SDGMethod getOwningMethod();
	//public abstract Set<SDGNode> getSourceNodes();
	//public abstract Set<SDGNode> getSinkNodes();
	boolean covers(SDGNode node);
	Collection<SDGNode> getAttachedNodes();
	Collection<SDGNode> getAttachedSourceNodes();
	Collection<SDGNode> getAttachedSinkNodes();

	SDGProgramPart getCoveringComponent(SDGNode node);

}
