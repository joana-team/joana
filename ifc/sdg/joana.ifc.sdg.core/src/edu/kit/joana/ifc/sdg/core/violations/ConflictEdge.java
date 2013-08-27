/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.violations;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;

/**
 * @author Martin Mohr
 */
public class ConflictEdge<T> {
	
	/** the source of this conflict edge */
	private final T source;
	
	/** the target of this conflict edge */
	private final T target;
	
	public ConflictEdge(T source, T target) {
		this.source = source;
		this.target = target;
	}

	/**
	 * @return the source of this conflict edge
	 */
	public T getSource() {
		return source;
	}

	/**
	 * @return the target of this conflict edge
	 */
	public T getTarget() {
		return target;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
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
		if (!(obj instanceof ConflictEdge)) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		ConflictEdge other = (ConflictEdge) obj;
		if (source == null) {
			if (other.source != null) {
				return false;
			}
		} else if (!source.equals(other.source)) {
			return false;
		}
		if (target == null) {
			if (other.target != null) {
				return false;
			}
		} else if (!target.equals(other.target)) {
			return false;
		}
		return true;
	}

	/**
	 * Creates a new ConflictEdge from a given SDG edge. It is assumed, that the source and
	 * the target of the given edge are of type {@link SecurityNode} and that the kind
	 * of the given edge is either {@link SDGEdge.Kind#CONFLICT_DATA} or 
	 * {@link SDGEdge.Kind#CONFLICT_ORDER}. You don't want to know what happens if one
	 * of these assumptions is not met!
	 * @param e edge to convert to a conflict edge
	 * @return conflict edge made from the given SDG edge
	 */
	public static ConflictEdge<SecurityNode> fromSDGEdge(SDGEdge e) {
		return new ConflictEdge<SecurityNode>((SecurityNode) e.getSource(), (SecurityNode) e.getTarget());
	}
}
