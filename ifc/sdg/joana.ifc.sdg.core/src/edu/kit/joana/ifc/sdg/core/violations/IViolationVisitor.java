/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.violations;

import edu.kit.joana.ifc.sdg.core.conc.DataConflict;
import edu.kit.joana.ifc.sdg.core.conc.OrderConflict;

/**
 * @author Add your name here.
 */
public interface IViolationVisitor<T> {
	public void visitIllegalFlow(IIllegalFlow<T> iFlow);
	public void visitDataConflict(DataConflict<T> dataConf);
	public void visitOrderConflict(OrderConflict<T> orderConf);
	public <L> void visitUnaryViolation(IUnaryViolation<T,L> unVio);
	public <L> void visitBinaryViolation(IBinaryViolation<T,L> binVio);
}
