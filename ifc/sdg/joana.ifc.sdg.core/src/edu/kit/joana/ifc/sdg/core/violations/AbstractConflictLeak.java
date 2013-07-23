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
import edu.kit.joana.util.Maybe;

/**
 * @author Martin Mohr
 */
public abstract class AbstractConflictLeak implements IConflictLeak {
	
	protected final SDGEdge confEdge;
	protected final String attackerLevel;
	protected final Maybe<SecurityNode> trigger;
	
	public AbstractConflictLeak(SDGEdge confEdge, String attackerLevel) {
		this(confEdge, attackerLevel, Maybe.<SecurityNode>nothing());
	}
	
	public AbstractConflictLeak(SDGEdge confEdge, String attackerLevel, Maybe<SecurityNode> trigger) {
		this.confEdge = confEdge;
		this.attackerLevel = attackerLevel;
		this.trigger = trigger;
	}
	
	public SDGEdge getConflictEdge() {
		return confEdge;
	}
	
	public Maybe<SecurityNode> getTrigger() {
		return trigger;
	}

}
