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
public abstract class SDGProgramPart {
	public abstract <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data);
	public abstract SDGMethod getOwningMethod();
	//public abstract Set<SDGNode> getSourceNodes();
	//public abstract Set<SDGNode> getSinkNodes();
	public abstract boolean covers(SDGNode node);
	public abstract Collection<SDGNode> getAttachedNodes();
	public abstract Collection<SDGNode> getAttachedSourceNodes();
	public abstract Collection<SDGNode> getAttachedSinkNodes();

	public abstract SDGProgramPart getCoveringComponent(SDGNode node);

}
