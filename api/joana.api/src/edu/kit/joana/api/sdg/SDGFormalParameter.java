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

public class SDGFormalParameter implements SDGProgramPart {

	private final SDGMethod owner;
	private final int index;
	private SDGNode inRoot = null;
	private SDGNode outRoot = null;
	private JavaType type = null;

	SDGFormalParameter(SDGMethod owner, int index) {
		this.owner = owner;
		this.index = index;
	}

	public void setInRoot(SDGNode newIn) {
		this.inRoot = newIn;
	}

	public void setOutRoot(SDGNode newOut) {
		this.outRoot = newOut;
	}

	public void setType(JavaType newType) {
		this.type = newType;
	}

	public JavaType getType() {
		return type;
	}

	public SDGNode getInRoot() {
		return inRoot;
	}

	public SDGNode getOutRoot() {
		return outRoot;
	}

	public String getName() {
		if (inRoot != null) {
			return inRoot.getLabel();
		}
		else {
			return outRoot.getLabel();
		}
	}

	@Override
	public String toString() {
		return "parameter " +  getName() + " of method "
				+ owner.getSignature().toHRString();
	}

	public int getIndex() {
		return index;
	}

	public SDGMethod getOwner() {
		return owner;
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
		result = prime * result + ((inRoot == null) ? 0 : inRoot.hashCode());
		result = prime * result + index;
		result = prime * result + ((outRoot == null) ? 0 : outRoot.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		if (!(obj instanceof SDGFormalParameter)) {
			return false;
		}
		SDGFormalParameter other = (SDGFormalParameter) obj;
		if (inRoot == null) {
			if (other.inRoot != null) {
				return false;
			}
		} else if (!inRoot.equals(other.inRoot)) {
			return false;
		}
		if (index != other.index) {
			return false;
		}
		if (outRoot == null) {
			if (other.outRoot != null) {
				return false;
			}
		} else if (!outRoot.equals(other.outRoot)) {
			return false;
		}

		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}
		return true;
	}

	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitParameter(this, data);
	}

	@Override
	public SDGMethod getOwningMethod() {
		return owner;
	}

	@Override
	public boolean covers(SDGNode node) {
		return SDGParameterUtils.psBackwardsReachable(node, this.inRoot, this.outRoot, getOwningMethod().getSDG());
	}

	@Override
	public Collection<SDGNode> getAttachedNodes() {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		if (inRoot != null) {
			ret.add(inRoot);
		}

		if (outRoot != null) {
			ret.add(outRoot);
		}

		return ret;
	}

	@Override
	public Collection<SDGNode> getAttachedSourceNodes() {
		Set<SDGNode> ret = new HashSet<SDGNode>();

		if (outRoot != null) {
			ret.add(outRoot);
		}

		return ret;	}

	@Override
	public Collection<SDGNode> getAttachedSinkNodes() {
		Set<SDGNode> ret = new HashSet<SDGNode>();
		if (inRoot != null) {
			ret.add(inRoot);
		}

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

}
