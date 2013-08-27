/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.views;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.conc.DataConflict;
import edu.kit.joana.ifc.sdg.core.conc.OrderConflict;
import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;
import edu.kit.joana.ifc.sdg.core.violations.ViolationMapper;



/**
 * @author Martin Mohr
 */
public class ViolationStringifier extends ViolationMapper<SecurityNode, String>{

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.ViolationMapper#mapIllegalFlow(edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow)
	 */
	@Override
	protected String mapIllegalFlow(IIllegalFlow<SecurityNode> iFlow) {
		return "Illicit Flow from Line " + iFlow.getSource().getSr() + " to Line " + iFlow.getSink().getSr() + ", visible for " + iFlow.getAttackerLevel();
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.ViolationMapper#mapDataConflict(edu.kit.joana.ifc.sdg.core.conc.DataConflict)
	 */
	@Override
	protected String mapDataConflict(DataConflict<SecurityNode> dc) {
		final int leakLineNo = dc.getInfluenced().getSr();
		final String attackerLevel = dc.getAttackerLevel();
		if (dc.getTrigger().isJust()) {
			final int triggerLineNo = dc.getTrigger().extract().getSr();
			return triggeredDataConflictDescription(triggerLineNo, leakLineNo, attackerLevel);
		} else {
			return untriggeredDataConflictDescription(leakLineNo, attackerLevel);
		}
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.ViolationMapper#mapOrderConflict(edu.kit.joana.ifc.sdg.core.conc.OrderConflict)
	 */
	@Override
	protected String mapOrderConflict(OrderConflict<SecurityNode> oc) {
		final int srcLineNo = oc.getConflictEdge().getSource().getSr();
		final int tgtLineNo = oc.getConflictEdge().getTarget().getSr();
		final String attackerLevel = oc.getAttackerLevel();
		if (oc.getTrigger().isJust()) {
			final int triggerLineNo = oc.getTrigger().extract().getSr();
			return triggeredOrderConflictDescription(srcLineNo, tgtLineNo, triggerLineNo, attackerLevel);
		} else {
			return untriggeredOrderConflictDescription(srcLineNo, tgtLineNo, attackerLevel);
		}
	}
	
	private String triggeredOrderConflictDescription(int srcLineNo, int tgtLineNo, int triggerLineNo, String attackerLevel) {
		return String.format("Probabilistic Order Channel between Lines %d and %d, leaking Line %d, visible for %s", srcLineNo, tgtLineNo, triggerLineNo, attackerLevel);
	}
	
	private String untriggeredOrderConflictDescription(int srcLineNo, int tgtLineNo, String attackerLevel) {
		return String.format("Probabilistic Order Channel between Lines %d and %d, visible for %s", srcLineNo, tgtLineNo, attackerLevel);
	}
	
	private String triggeredDataConflictDescription(int triggerLineNo, int leakLineNo, String attackerLevel) {
		return String.format("Probabilistic Data Channel from Line %d to %d, visible for %s", triggerLineNo, leakLineNo, attackerLevel);
	}
	
	private String untriggeredDataConflictDescription(int leakLineNo, String attackerLevel) {
		return String.format("Probabilistic Data Channel at Line %d, visible for %s, possibly leaking unknown secret information", leakLineNo, attackerLevel);
	}
}
