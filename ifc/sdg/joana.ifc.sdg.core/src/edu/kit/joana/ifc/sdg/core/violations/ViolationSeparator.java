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


/**
 * @author Martin Mohr
 */
public class ViolationSeparator<T> implements IViolationVisitor<T> {
	
	private Collection<IIllegalFlow<T>> iFlows = new LinkedList<IIllegalFlow<T>>();
	private Collection<DataConflict<T>> dConfs = new LinkedList<DataConflict<T>>();
	private Collection<OrderConflict<T>> oConfs = new LinkedList<OrderConflict<T>>();
 	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitIllegalFlow(edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow)
	 */
	@Override
	public void visitIllegalFlow(IIllegalFlow<T> iFlow) {
		iFlows.add(iFlow);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitDataConflict(edu.kit.joana.ifc.sdg.core.conc.DataConflict)
	 */
	@Override
	public void visitDataConflict(DataConflict<T> dataConf) {
		dConfs.add(dataConf);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitOrderConflict(edu.kit.joana.ifc.sdg.core.conc.OrderConflict)
	 */
	@Override
	public void visitOrderConflict(OrderConflict<T> orderConf) {
		oConfs.add(orderConf);
	}
	
	
	public void separate(Collection<? extends IViolation<T>> vios) {
		reset();
		for (IViolation<T> v : vios) {
			v.accept(this);
		}
	}
	
	public void reset() {
		iFlows.clear();
		dConfs.clear();
		oConfs.clear();
	}
	
	public Collection<IIllegalFlow<T>> getIllegalFlows() {
		return iFlows;
	}
	
	public Collection<DataConflict<T>> getDataConflicts() {
		return dConfs;
	}
	
	public Collection<OrderConflict<T>> getOrderConflicts() {
		return oConfs;
	}
	
}
