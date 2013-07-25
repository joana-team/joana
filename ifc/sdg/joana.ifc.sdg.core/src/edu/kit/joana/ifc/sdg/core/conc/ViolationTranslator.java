/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.conc;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;

/**
 * @author Martin Mohr
 */
public class ViolationTranslator extends ViolationMapper<SecurityNode, ClassifiedViolation> {
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.conc.ViolationMapper#mapIllegalFlow(edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow)
	 */
	@Override
	protected ClassifiedViolation mapIllegalFlow(IIllegalFlow<SecurityNode> iFlow) {
		return ClassifiedViolation.createViolation(iFlow.getSink(), iFlow.getSource(), iFlow.getAttackerLevel());
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.conc.ViolationMapper#mapDataConflict(edu.kit.joana.ifc.sdg.core.conc.DataConflict)
	 */
	@Override
	protected ClassifiedViolation mapDataConflict(DataConflict<SecurityNode> dc) {
		SecurityNode src = null;
    	if (dc.getTrigger().isJust()) {
    		src = dc.getTrigger().extract();
    	}
    	return ClassifiedViolation.createViolation(dc.getInfluenced(), src, dc.getAttackerLevel());
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.conc.ViolationMapper#mapOrderConflict(edu.kit.joana.ifc.sdg.core.conc.OrderConflict)
	 */
	@Override
	protected ClassifiedViolation mapOrderConflict(OrderConflict<SecurityNode> oc) {
		return ClassifiedViolation.createViolation((SecurityNode) oc.getConflictEdge().getTarget(), (SecurityNode) oc.getConflictEdge().getSource(), oc.getAttackerLevel());
	}
}
