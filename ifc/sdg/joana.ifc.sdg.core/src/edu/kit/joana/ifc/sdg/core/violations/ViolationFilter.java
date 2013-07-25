/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.violations;

import java.util.Collection;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.core.conc.DataConflict;
import edu.kit.joana.ifc.sdg.core.conc.OrderConflict;


public abstract class ViolationFilter<T> implements IViolationVisitor<T> {

	private boolean lastResult;
	
	public Collection<? extends IViolation<T>> filter(Collection<? extends IViolation<T>> coll) {
		Collection<IViolation<T>> ret = new LinkedList<IViolation<T>>();
		for (IViolation<T> vio : coll) {
			if (accept(vio)) {
				ret.add(vio);
			}
		}

		return ret;
	}
	
	protected boolean accept(IViolation<T> vio) {
		vio.accept(this);
		return lastResult;
	}
	
	protected boolean acceptIllegalFlow(IIllegalFlow<T> iFlow) { return true; }
	protected boolean acceptDataConflict(DataConflict<T> dataConf) { return true; }
	protected boolean acceptOrderConflict(OrderConflict<T> orderConf) { return true; }
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitIllegalFlow(edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow)
	 */
	@Override
	public void visitIllegalFlow(IIllegalFlow<T> iFlow) {
		lastResult = acceptIllegalFlow(iFlow);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitDataConflict(edu.kit.joana.ifc.sdg.core.conc.DataConflict)
	 */
	@Override
	public void visitDataConflict(DataConflict<T> dataConf) {
		lastResult = acceptDataConflict(dataConf);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitOrderConflict(edu.kit.joana.ifc.sdg.core.conc.OrderConflict)
	 */
	@Override
	public void visitOrderConflict(OrderConflict<T> orderConf) {
		lastResult = acceptOrderConflict(orderConf);
	}
	
}