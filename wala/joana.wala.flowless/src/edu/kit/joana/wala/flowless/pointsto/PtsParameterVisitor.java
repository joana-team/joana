/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.pointsto;

import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.MergedPtsParameter;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.ArrayFieldParameter;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.NormalFieldParameter;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.RootParameter;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public interface PtsParameterVisitor {

	public void visit(ArrayFieldParameter array);
	public void visit(NormalFieldParameter field);
	public void visit(MergedPtsParameter merged);
	public void visit(RootParameter root);

}
