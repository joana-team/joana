/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.conc;

import java.util.Collection;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor;

/**
 * TODO: @author Add your name here.
 */
public class ViolationTranslator implements IViolationVisitor {
	
	private Collection<ClassifiedViolation> translated = new LinkedList<ClassifiedViolation>();
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitIllegalFlow(edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow)
	 */
	@Override
	public void visitIllegalFlow(IIllegalFlow iFlow) {
		translated.add(ClassifiedViolation.createViolation(iFlow.getSink(), iFlow.getSource(), iFlow.getAttackerLevel()));
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitDataConflict(edu.kit.joana.ifc.sdg.core.conc.DataConflict)
	 */
	@Override
	public void visitDataConflict(DataConflict dataConf) {
		SecurityNode src = null;
    	if (dataConf.getTrigger().isJust()) {
    		src = dataConf.getTrigger().extract();
    	}
    	translated.add(ClassifiedViolation.createViolation(dataConf.getInfluenced(), src, dataConf.getAttackerLevel()));
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitOrderConflict(edu.kit.joana.ifc.sdg.core.conc.OrderConflict)
	 */
	@Override
	public void visitOrderConflict(OrderConflict orderConf) {
		translated.add(ClassifiedViolation.createViolation((SecurityNode) orderConf.getConflictEdge().getTarget(), (SecurityNode) orderConf.getConflictEdge().getSource(), orderConf.getAttackerLevel()));
	}
	
	public void reset() {
		translated.clear();
	}
	
	public Collection<ClassifiedViolation> translate(Collection<? extends IViolation> vios) {
		reset();
		for (IViolation vio : vios) {
			vio.accept(this);
		}
		return new LinkedList<ClassifiedViolation>(translated);
	}
}
