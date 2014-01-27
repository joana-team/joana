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


public class SDGPhi implements SDGProgramPart {

	private SDGMethod owner;
	private SDGNode node;

	SDGPhi(SDGMethod owner, SDGNode node) {
		this.owner = owner;
		this.node = node;
	}

	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitPhi(this, data);
	}

	@Override
	public SDGMethod getOwningMethod() {
		return owner;
	}

	@Override
	public String toString() {
		return node.getLabel();
	}

	@Override
	public boolean covers(SDGNode node) {
		return this.node.equals(node);
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
		} else {
			return null;
		}
	}
}
