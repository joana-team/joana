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

/**
 * @author Martin Mohr
 */
public class SDGActualParameter extends SDGProgramPart {
	private final SDGCall owningCall;
	private final int index;
	private SDGNode inRoot = null;
	private SDGNode outRoot = null;
	private final JavaType type;
	
	SDGActualParameter(SDGCall owningCall, int index, JavaType type) {
		this.owningCall = owningCall;
		this.index = index;
		this.type = type;
	}
	
    void setInRoot(SDGNode inRoot) {
		if (this.inRoot != null && !this.inRoot.equals(inRoot)) {
			throw new IllegalStateException("inRoot was initialized more than once: " + "paramIndex = " + this.index + "this.inRoot = " + this.inRoot + " " + "new inRoot = " + inRoot);
		}
		this.inRoot = inRoot;
	}
	
	void setOutRoot(SDGNode outRoot) {
		if (this.outRoot != null && !this.outRoot.equals(outRoot)) {
			throw new IllegalStateException("outRoot was initialized more than once: " + "paramIndex = " + this.index + "this.outRoot = " + this.outRoot + " " + "new outRoot = " + outRoot);
		}
		this.outRoot = outRoot;
	}
	
	public int getIndex() {
		return index;
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

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#acceptVisitor(edu.kit.joana.api.sdg.SDGProgramPartVisitor, java.lang.Object)
	 */
	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitActualParameter(this, data);
	}
	
	public SDGCall getOwningCall() {
		return owningCall;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#getOwningMethod()
	 */
	@Override
	public SDGMethod getOwningMethod() {
		return owningCall.getOwningMethod();
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#covers(edu.kit.joana.ifc.sdg.graph.SDGNode)
	 */
	@Override
	public boolean covers(SDGNode node) {
		return node.equals(inRoot) || node.equals(outRoot);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#getAttachedNodes()
	 */
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

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#getAttachedSourceNodes()
	 */
	@Override
	public Collection<SDGNode> getAttachedSourceNodes() {
		Set<SDGNode> ret = new HashSet<SDGNode>();

		if (outRoot != null) {
			ret.add(outRoot);
		}

		return ret;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#getAttachedSinkNodes()
	 */
	@Override
	public Collection<SDGNode> getAttachedSinkNodes() {
		Set<SDGNode> ret = new HashSet<SDGNode>();

		if (inRoot != null) {
			ret.add(inRoot);
		}

		return ret;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#getCoveringComponent(edu.kit.joana.ifc.sdg.graph.SDGNode)
	 */
	@Override
	public SDGProgramPart getCoveringComponent(SDGNode node) {
		if (covers(node)) {
			return this;
		} else {
			return null;
		}
	}

}
