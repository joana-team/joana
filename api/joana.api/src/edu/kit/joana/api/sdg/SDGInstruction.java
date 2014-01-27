/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import java.util.Collection;
import java.util.Collections;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


public class SDGInstruction implements SDGProgramPart,
Comparable<SDGInstruction> {

	private final SDGNode rootNode;
	private final SDGMethod owner;

	SDGInstruction(SDGMethod owner, SDGNode rootNode, int index) {
		this.rootNode = rootNode;
		this.owner = owner;
	}

	public SDGNode getNode() {
		return rootNode;
	}

	public SDGMethod getOwner() {
		return owner;
	}

	public int getBytecodeIndex() {
		return rootNode.getBytecodeIndex();
	}

	@Override
	public String toString() {
		return "(" + owner.getSignature().toHRString() + ":"
		+ rootNode.getBytecodeIndex() + ") " + rootNode.getLabel();
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
		result = prime * result
		+ ((rootNode == null) ? 0 : rootNode.hashCode());
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
		if (!(obj instanceof SDGInstruction)) {
			return false;
		}
		SDGInstruction other = (SDGInstruction) obj;
		if (rootNode == null) {
			if (other.rootNode != null) {
				return false;
			}
		} else if (!rootNode.equals(other.rootNode)) {
			return false;
		}
		return true;
	}

	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitInstruction(this, data);
	}

	@Override
	public int compareTo(SDGInstruction arg0) {
		return getBytecodeIndex() - arg0.getBytecodeIndex();
	}

	@Override
	public SDGMethod getOwningMethod() {
		return owner;
	}

	@Override
	public boolean covers(SDGNode node) {
		return rootNode.equals(node);
	}

	@Override
	public Collection<SDGNode> getAttachedNodes() {
		return Collections.singleton(rootNode);
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
		} else {
			return null;
		}
	}

	

	/**
	 * Returns whether this instruction is a call instruction.
	 * @return {@code true}, if this instruction is a call instruction, {@code false} otherwise
	 */
	public boolean isCall() {
		return rootNode.getKind() == SDGNode.Kind.CALL;
	}

}
