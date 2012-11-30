/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.spec.ast;

import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public interface AstElement {

	public static enum Type { ALIAS_PRIMITIVE, ALIAS_BOOL, ALIAS_INFER, UNIQUE, FLOW, PURE, IFC, PARAM, PARAM_OPT_LIST };

	Type getType();

	void accept(FlowAstVisitor visitor) throws FlowAstException;

}
