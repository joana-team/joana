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

public class SDGAttribute implements SDGProgramPart {

	private final SDGClass owningClass;
	private final JavaType declaringType;
	private final String name;
	private final Set<SDGNode> srcNodes = new HashSet<SDGNode>();
	private final Set<SDGNode> snkNodes = new HashSet<SDGNode>();

	SDGAttribute(SDGClass declaringClass, String name, Set<SDGNode> srcNodes, Set<SDGNode> snkNodes) {
		this.owningClass = declaringClass;
		this.declaringType = declaringClass.getTypeName();
		this.srcNodes.addAll(srcNodes);
		this.snkNodes.addAll(snkNodes);
		this.name = name;
	}

	public String getName() {
		int offset = this.name.lastIndexOf('.');
		return name.substring(offset + 1);
	}

	public String getType() {
		final SDGNode node;
		if (!srcNodes.isEmpty()) {
			node = srcNodes.iterator().next();
		} else {
			node = snkNodes.iterator().next();
		}
		assert node != null;
		return JavaType.parseSingleTypeFromString(node.getType(), Format.BC).toHRString();
	}

	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitAttribute(this, data);
	}

	@Override
	public SDGMethod getOwningMethod() {
		return null;
	}

	@Override
	public String toString() {
		return owningClass.toString() + "." + getName();// + srcNodes + "/" + snkNodes;
	}

	@Override
	public boolean covers(SDGNode node) {
		return srcNodes.contains(node) || snkNodes.contains(node);
	}

	@Override
	public Collection<SDGNode> getAttachedNodes() {
		final Set<SDGNode> ret = new HashSet<SDGNode>();
		ret.addAll(srcNodes);
		ret.addAll(snkNodes);

		return ret;
	}

	@Override
	public Collection<SDGNode> getAttachedSourceNodes() {
		final Set<SDGNode> ret = new HashSet<SDGNode>();
		ret.addAll(srcNodes);
		return ret;
	}

	@Override
	public Collection<SDGNode> getAttachedSinkNodes() {
		final Set<SDGNode> ret = new HashSet<SDGNode>();
		ret.addAll(snkNodes);
		return ret;
	}

	@Override
	public SDGProgramPart getCoveringComponent(SDGNode node) {
		if (covers(node)) {
			return this;
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((declaringType == null) ? 0 : declaringType.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/* (non-Javadoc)
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
		if (!(obj instanceof SDGAttribute)) {
			return false;
		}
		SDGAttribute other = (SDGAttribute) obj;
		if (declaringType == null) {
			if (other.declaringType != null) {
				return false;
			}
		} else if (!declaringType.equals(other.declaringType)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}
}
