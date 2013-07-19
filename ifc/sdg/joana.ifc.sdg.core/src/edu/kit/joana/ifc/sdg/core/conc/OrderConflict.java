/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.conc;

import edu.kit.joana.ifc.sdg.core.violations.AbstractConflict;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.util.Maybe;

class OrderConflict extends AbstractConflict {

	private SDGEdge confEdge;
	private String attackerLevel;
	private Maybe<SDGNode> trigger;
	
	private final String untriggeredTemplate = "Indefinite execution order between nodes %s and %s, visible for %s";
	private final String triggeredTemplate = "Indefinite execution order between nodes %s and %s, visible for %s, may be influenced by %s";
	
	public OrderConflict(SDGEdge confEdge, String attackerLevel, Maybe<SDGNode> trigger) {
		this.confEdge = confEdge;
		this.attackerLevel = attackerLevel;
		this.trigger = trigger;
	}
	
	public OrderConflict(SDGEdge confEdge, String attackerLevel) {
		this(confEdge, attackerLevel, Maybe.<SDGNode>nothing());
	}

	@Override
	public String toString() {
		if (trigger.isNothing()) {
			return String.format(untriggeredTemplate, confEdge.getSource(), confEdge.getTarget(), attackerLevel);
		} else {
			return String.format(triggeredTemplate, confEdge.getSource(), confEdge.getTarget(), attackerLevel, trigger.extract());
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
		
		if (!(obj instanceof OrderConflict)) {
			return false;
		}
		OrderConflict other = (OrderConflict) obj;
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
		if (trigger == null) {
			if (other.trigger != null) {
				return false;
			}
		} else if (!trigger.equals(other.trigger)) {
			return false;
		}
		return true;
	}
}