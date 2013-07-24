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
import edu.kit.joana.util.Maybe;

public abstract class ViolationPartialMapper<T> implements IViolationVisitor {
	private Maybe<T> lastResult;
	
	public Collection<T> map(Collection<? extends IViolation> coll) {
		Collection<T> ret = new LinkedList<T>();
		for (IViolation vio : coll) {
			Maybe<T> next = map(vio);
			if (!next.isNothing()) {
				ret.add(next.extract());
			}
		}

		return ret;
	}
	
	protected Maybe<T> map(IViolation vio) {
		vio.accept(this);
		return lastResult;
	}
	
	protected Maybe<T> maybeMapIllegalFlow(IIllegalFlow iFlow) { return Maybe.nothing(); };
	protected Maybe<T> maybeMapDataConflict(DataConflict dc) { return Maybe.nothing(); };
	protected Maybe<T> maybeMapOrderConflict(OrderConflict oc) { return Maybe.nothing(); };

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitIllegalFlow(edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow)
	 */
	@Override
	public final void visitIllegalFlow(IIllegalFlow iFlow) {
		lastResult = maybeMapIllegalFlow(iFlow);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitDataConflict(edu.kit.joana.ifc.sdg.core.conc.DataConflict)
	 */
	@Override
	public final void visitDataConflict(DataConflict dataConf) {
		lastResult = maybeMapDataConflict(dataConf);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor#visitOrderConflict(edu.kit.joana.ifc.sdg.core.conc.OrderConflict)
	 */
	@Override
	public final void visitOrderConflict(OrderConflict orderConf) {
		lastResult = maybeMapOrderConflict(orderConf);
	}
}