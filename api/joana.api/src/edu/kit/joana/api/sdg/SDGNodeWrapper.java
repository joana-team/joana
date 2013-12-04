/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * @author Martin Mohr
 */
public abstract class SDGNodeWrapper implements SDGProgramPart {

	protected SDGNode node;
	protected SDGMethod owningMethod;
	
	public SDGNodeWrapper(SDGNode node, SDGMethod owningMethod) {
		this.node = node;
		this.owningMethod = owningMethod;
	}
	
	public SDGNode getNode() {
		return node;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SDGNodeWrapper)) {
			return false;
		}
		SDGNodeWrapper other = (SDGNodeWrapper) obj;
		if (node == null) {
			if (other.node != null) {
				return false;
			}
		} else if (!node.equals(other.node)) {
			return false;
		}
		return true;
	}

	@Override
	public boolean covers(SDGNode node) {
		return node.equals(this.node);
	}

	@Override
	public Collection<SDGNode> getAttachedNodes() {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		ret.add(this.node);
		return ret;
	}

	@Override
	public Collection<SDGNode> getAttachedSourceNodes() {
		return getAttachedNodes();
	}

	@Override
	public Collection<SDGNode> getAttachedSinkNodes() {
		return getAttachedNodes();
	}

	@Override
	public SDGProgramPart getCoveringComponent(SDGNode node) {
		if (covers(node)) {
			return this;
		}
	
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#getOwningMethod()
	 */
	@Override
	public SDGMethod getOwningMethod() {
		return owningMethod;
	}

}