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
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.ifc.sdg.util.JavaType.Format;

public class SDGMethodExitNode extends SDGProgramPart {

	private SDGNode node;
	private JavaType type;
	private SDGMethod owner;

	SDGMethodExitNode(SDGNode node, SDGMethod owner) {
		this.node = node;
		this.owner = owner;
		this.type = JavaType.parseSingleTypeFromString(node.getType(), Format.BC);
	}

	public SDGMethod getOwner() {
		return owner;
	}

	public SDGNode getExitNode() {
		return node;
	}

	public JavaType getType() {
		return type;
	}

	@Override
	/*
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "exit of method " + getOwningMethod().getSignature().toHRString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SDGMethodExitNode)) {
			return false;
		}
		SDGMethodExitNode other = (SDGMethodExitNode) obj;
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
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitExit(this, data);
	}

	@Override
	public SDGMethod getOwningMethod() {
		return owner;
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
}
