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
 * Separates a collection of IViolations into the different kinds of violation. First,
 * run {@link #separate(Collection)} on the collection to be separated. Afterwards,
 * you can use the respective getter methods to access the different groups.
 * @author Martin Mohr
 */
public class ViolationSeparator<T> implements IViolationVisitor<T> {

	private final Collection<IIllegalFlow<T>> iFlows = new LinkedList<IIllegalFlow<T>>();
	private final Collection<DataConflict<T>> dConfs = new LinkedList<DataConflict<T>>();
	private final Collection<OrderConflict<T>> oConfs = new LinkedList<OrderConflict<T>>();
	private final Collection<IUnaryViolation<T,?>> unVios = new LinkedList<IUnaryViolation<T,?>>();
	private final Collection<IBinaryViolation<T,?>> binVios = new LinkedList<IBinaryViolation<T,?>>();
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
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitUnaryViolation(edu.kit.joana.ifc.sdg.core.violations.IUnaryViolation)
	 */
	@Override
	public <L> void visitUnaryViolation(IUnaryViolation<T, L> unVio) {
		unVios.add(unVio);
	}

	@Override
	public <L> void visitBinaryViolation(IBinaryViolation<T, L> binVio) {
		binVios.add(binVio);
	}
	/**
	 * Separates the given collection of violations into the different groups. Use
	 * appropriate getter method to access the individual groups.
	 * @param vios collection of violations to separate
	 */
	public void separate(Collection<? extends IViolation<T>> vios) {
		reset();
		for (IViolation<T> v : vios) {
			v.accept(this);
		}
	}
	
	/**
	 * Resets this violation separator. Call this method to re-use this object for another
	 * collection of violations.
	 */
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
