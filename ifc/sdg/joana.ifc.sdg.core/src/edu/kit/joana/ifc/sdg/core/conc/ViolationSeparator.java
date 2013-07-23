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

/**
 * @author Martin Mohr
 */
public class ViolationSeparator implements IViolationVisitor {
	
	private Collection<IIllegalFlow> iFlows = new LinkedList<IIllegalFlow>();
	private Collection<DataConflict> dConfs = new LinkedList<DataConflict>();
	private Collection<OrderConflict> oConfs = new LinkedList<OrderConflict>();
 	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitIllegalFlow(edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow)
	 */
	@Override
	public void visitIllegalFlow(IIllegalFlow iFlow) {
		iFlows.add(iFlow);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitDataConflict(edu.kit.joana.ifc.sdg.core.conc.DataConflict)
	 */
	@Override
	public void visitDataConflict(DataConflict dataConf) {
		dConfs.add(dataConf);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitOrderConflict(edu.kit.joana.ifc.sdg.core.conc.OrderConflict)
	 */
	@Override
	public void visitOrderConflict(OrderConflict orderConf) {
		oConfs.add(orderConf);
	}
	
	
	public void sort(Collection<? extends IViolation> vios) {
		reset();
		for (IViolation v : vios) {
			v.accept(this);
		}
	}
	
	public void reset() {
		iFlows.clear();
		dConfs.clear();
		oConfs.clear();
	}
	
	public Collection<IIllegalFlow> getIllegalFlows() {
		return iFlows;
	}
	
	public Collection<DataConflict> getDataConflicts() {
		return dConfs;
	}
	
	public Collection<OrderConflict> getOrderConflicts() {
		return oConfs;
	}
	
}
