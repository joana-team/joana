/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.spec.ast;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public interface FlowAstVisitor {

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static abstract class FlowAstException extends Exception {

		private static final long serialVersionUID = -1684761687817215200L;

		public FlowAstException(String message) {
			super(message);
		}

		public FlowAstException(Throwable t) {
			super(t);
		}

		public FlowAstException(String message, Throwable t) {
			super(message, t);
		}

	}

	void visit(InferableAliasStmt alias) throws FlowAstException;
	void visit(PrimitiveAliasStmt alias) throws FlowAstException;
	void visit(UniqueStmt unique) throws FlowAstException;
	void visit(BooleanAliasStmt alias) throws FlowAstException;
	void visit(SimpleParameter param) throws FlowAstException;
	void visit(ParameterOptList param) throws FlowAstException;
	void visit(IFCStmt ifc) throws FlowAstException;
	void visit(ExplicitFlowStmt ifc) throws FlowAstException;
	void visit(PureStmt ifc) throws FlowAstException;

}
