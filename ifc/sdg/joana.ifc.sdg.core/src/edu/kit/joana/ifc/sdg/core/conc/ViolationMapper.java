/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.conc;

import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;
import edu.kit.joana.util.Maybe;

/**
 * @author Add your name here.
 */
public abstract class ViolationMapper<T> extends ViolationPartialMapper<T> {

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.conc.ViolationPartialMapper#mapIllegalFlow(edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow)
	 */
	@Override
	protected final Maybe<T> maybeMapIllegalFlow(IIllegalFlow iFlow) {
		return Maybe.just(mapIllegalFlow(iFlow));
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.conc.ViolationPartialMapper#mapDataConflict(edu.kit.joana.ifc.sdg.core.conc.DataConflict)
	 */
	@Override
	protected final Maybe<T> maybeMapDataConflict(DataConflict dc) {
		return Maybe.just(mapDataConflict(dc));
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.conc.ViolationPartialMapper#mapOrderConflict(edu.kit.joana.ifc.sdg.core.conc.OrderConflict)
	 */
	@Override
	protected final Maybe<T> maybeMapOrderConflict(OrderConflict oc) {
		return Maybe.just(mapOrderConflict(oc));
	}
	
	protected abstract T mapIllegalFlow(IIllegalFlow iFlow);
	protected abstract T mapDataConflict(DataConflict dc);
	protected abstract T mapOrderConflict(OrderConflict oc);
}
