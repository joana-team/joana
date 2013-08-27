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
import edu.kit.joana.util.Maybe;

public abstract class ViolationPartialMapper<T,U> implements IViolationVisitor<T> {
	private Maybe<U> lastResult;
	
	public Maybe<U> maybeMapSingle(IViolation<T> x) {
		return map(x);
	}
	
	public Collection<U> map(Collection<? extends IViolation<T>> coll) {
		Collection<U> ret = new LinkedList<U>();
		for (IViolation<T> vio : coll) {
			Maybe<U> next = map(vio);
			if (!next.isNothing()) {
				ret.add(next.extract());
			}
		}

		return ret;
	}
	
	protected Maybe<U> map(IViolation<T> vio) {
		vio.accept(this);
		return lastResult;
	}
	
	protected Maybe<U> maybeMapIllegalFlow(IIllegalFlow<T> iFlow) { return Maybe.nothing(); };
	protected Maybe<U> maybeMapDataConflict(DataConflict<T> dc) { return Maybe.nothing(); };
	protected Maybe<U> maybeMapOrderConflict(OrderConflict<T> oc) { return Maybe.nothing(); };

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitIllegalFlow(edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow)
	 */
	@Override
	public final void visitIllegalFlow(IIllegalFlow<T> iFlow) {
		lastResult = maybeMapIllegalFlow(iFlow);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitDataConflict(edu.kit.joana.ifc.sdg.core.conc.DataConflict)
	 */
	@Override
	public final void visitDataConflict(DataConflict<T> dataConf) {
		lastResult = maybeMapDataConflict(dataConf);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitOrderConflict(edu.kit.joana.ifc.sdg.core.conc.OrderConflict)
	 */
	@Override
	public final void visitOrderConflict(OrderConflict<T> orderConf) {
		lastResult = maybeMapOrderConflict(orderConf);
	}
}