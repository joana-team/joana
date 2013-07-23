/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.conc;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.AbstractConflictLeak;
import edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.util.Maybe;

public class DataConflict extends AbstractConflictLeak {

	private final SecurityNode influenced;
	private final Maybe<SecurityNode> trigger;
	
	private final String untriggeredTemplate = "Data conflict between nodes %s and %s, may influence the behaviour of node %s, which is visible for %s";
	private final String triggeredTemplate = "Data conflict between nodes %s and %s, may reveal something about node %s by influencing the behaviour of node %s, which is visible for %s";
	
	public DataConflict(SDGEdge confEdge, SecurityNode influenced,
			String attackerLevel, Maybe<SecurityNode> trigger) {
		super(confEdge, attackerLevel);
		this.influenced = influenced;
		this.trigger = trigger;
	}
	
	public DataConflict(SDGEdge confEdge, SecurityNode influenced,
			String attackerLevel) {
		this(confEdge, influenced, attackerLevel, Maybe.<SecurityNode>nothing());
	}
	
	public SecurityNode getInfluenced() {
		return influenced;
	}

	@Override
	public String toString() {
		if (trigger.isNothing()) {
			return String.format(untriggeredTemplate, confEdge.getSource(), confEdge.getTarget(), influenced, attackerLevel);
		} else {
			return String.format(triggeredTemplate, confEdge.getSource(), confEdge.getTarget(), trigger.extract(), influenced, attackerLevel);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 0;
		result = prime * result + ((attackerLevel == null) ? 0 : attackerLevel.hashCode());
		result = prime * result + ((confEdge == null) ? 0 : confEdge.hashCode());
		result = prime * result + ((influenced == null) ? 0 : influenced.hashCode());
		result = prime * result + ((trigger == null) ? 0 : trigger.hashCode());
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
	
		if (!(obj instanceof DataConflict)) {
			return false;
		}
		DataConflict other = (DataConflict) obj;
		if (attackerLevel == null) {
			if (other.attackerLevel != null) {
				return false;
			}
		} else if (!attackerLevel.equals(other.attackerLevel)) {
			return false;
		}
		if (confEdge == null) {
			if (other.confEdge != null) {
				return false;
			}
		} else if (!confEdge.equals(other.confEdge)) {
			return false;
		}
		if (influenced == null) {
			if (other.influenced != null) {
				return false;
			}
		} else if (!influenced.equals(other.influenced)) {
			return false;
		}
		if (trigger == null) {
			if (other.trigger != null) {
				return false;
			}
		} else if (!trigger.equals(other.trigger)) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IConflictLeak#getAttackerLevel()
	 */
	@Override
	public String getAttackerLevel() {
		return attackerLevel;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolation#accept(edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor)
	 */
	@Override
	public void accept(IViolationVisitor v) {
		v.visitDataConflict(this);
	}
}