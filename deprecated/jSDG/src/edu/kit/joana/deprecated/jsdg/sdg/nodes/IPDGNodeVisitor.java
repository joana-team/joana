/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;


/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public interface IPDGNodeVisitor {

	public void visitParameter(AbstractParameterNode node);
	public void visitCall(CallNode node);
	public void visitCatch(CatchNode node);
	public void visitEntry(EntryNode node);
	public void visitExpression(ExpressionNode node);
	public void visitNormal(NormalNode node);
	public void visitPredicate(PredicateNode node);
	public void visitSync(SyncNode node);
	public void visitPhiValue(PhiValueNode node);
	public void visitConstPhiValue(ConstantPhiValueNode node);

}
