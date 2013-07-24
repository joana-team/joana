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

import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor;

public abstract class ViolationFilter implements IViolationVisitor {

	private boolean lastResult;
	
	public Collection<? extends IViolation> filter(Collection<? extends IViolation> coll) {
		Collection<IViolation> ret = new LinkedList<IViolation>();
		for (IViolation vio : coll) {
			if (accept(vio)) {
				ret.add(vio);
			}
		}

		return ret;
	}
	
	protected boolean accept(IViolation vio) {
		vio.accept(this);
		return lastResult;
	}
	
	protected boolean acceptIllegalFlow(IIllegalFlow iFlow) { return true; }
	protected boolean acceptDataConflict(DataConflict dataConf) { return true; }
	protected boolean acceptOrderConflict(OrderConflict orderConf) { return true; }
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitIllegalFlow(edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow)
	 */
	@Override
	public void visitIllegalFlow(IIllegalFlow iFlow) {
		lastResult = acceptIllegalFlow(iFlow);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitDataConflict(edu.kit.joana.ifc.sdg.core.conc.DataConflict)
	 */
	@Override
	public void visitDataConflict(DataConflict dataConf) {
		lastResult = acceptDataConflict(dataConf);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitOrderConflict(edu.kit.joana.ifc.sdg.core.conc.OrderConflict)
	 */
	@Override
	public void visitOrderConflict(OrderConflict orderConf) {
		lastResult = acceptOrderConflict(orderConf);
	}
	
}