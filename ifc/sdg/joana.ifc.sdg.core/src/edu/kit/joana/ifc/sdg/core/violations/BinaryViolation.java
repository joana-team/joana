/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.violations;

/**
 * TODO: @author Add your name here.
 */
public class BinaryViolation<T,L> implements IBinaryViolation<T, L> {
	private final T node;
	private final T influencedBy;
	private final L attackerLevel;

	public BinaryViolation(T node, T influencedBy, L attackerLevel) {
		this.node = node;
		this.influencedBy = influencedBy;
		this.attackerLevel = attackerLevel;
	}

	@Override
	public void accept(IViolationVisitor<T> v) {
		v.visitBinaryViolation(this);
	}

	@Override
	public T getNode() {
		return node;
	}

	@Override
	public T getInfluencedBy() {
		return influencedBy;
	}

	@Override
	public L getAttackerLevel() {
		return attackerLevel;
	}

}
