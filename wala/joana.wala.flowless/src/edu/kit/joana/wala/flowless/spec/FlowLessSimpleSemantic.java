/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.spec;

import java.util.List;

import edu.kit.joana.wala.flowless.spec.ast.BooleanAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.ExplicitFlowStmt;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor;
import edu.kit.joana.wala.flowless.spec.ast.FlowStmt;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.InferableAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.Parameter;
import edu.kit.joana.wala.flowless.spec.ast.ParameterOptList;
import edu.kit.joana.wala.flowless.spec.ast.PrimitiveAliasStmt;
import edu.kit.joana.wala.flowless.spec.ast.PureStmt;
import edu.kit.joana.wala.flowless.spec.ast.SimpleParameter;
import edu.kit.joana.wala.flowless.spec.ast.SimpleParameter.Part;
import edu.kit.joana.wala.flowless.spec.ast.UniqueStmt;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo.ParamInfo;
import edu.kit.joana.wala.util.ParamNum;
import edu.kit.joana.wala.util.ParamNum.PType;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class FlowLessSimpleSemantic implements FlowAstVisitor {

	private final MethodInfo method;

	private FlowLessSimpleSemantic(MethodInfo method) {
		this.method = method;
	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class SemanticException extends FlowAstException {

		private static final long serialVersionUID = -3375454090554575566L;

		private final int lineNr;

		public SemanticException(String message, final int lineNr) {
			super(message);
			this.lineNr = lineNr;
		}

		public int getLineNr() {
			return lineNr;
		}

	}

	private int currentLine = 0;

	public static void check(MethodInfo m) throws FlowAstException {
		FlowLessSimpleSemantic flss = new FlowLessSimpleSemantic(m);

		for (IFCStmt ifc : m.getIFCStmts()) {
			ifc.accept(flss);
		}
	}

	@Override
	public void visit(PrimitiveAliasStmt alias) throws FlowAstException {
		for (Parameter p : alias.getParams()) {
			p.accept(this);
		}
	}

	@Override
	public void visit(BooleanAliasStmt alias) throws FlowAstException  {
		alias.left.accept(this);
		alias.right.accept(this);
	}

	/**
	 * Returns parameter number in ParamNumberUtil format. 
	 */
	private ParamNum searchParamWithName(final String name) {
		if (name.equals("this") && !method.isStatic()) {
			return ParamNum.createSpecial(PType.THIS_VAL);
		}

		final List<ParamInfo> params =  method.getParameters();
		for (int index = 0; index < params.size(); index++) {
			final ParamInfo pi = params.get(index);
			if (pi.name.equals(name)) {
				return ParamNum.fromIMethod(method.isStatic(), index);
			}
		}

		return ParamNum.createSpecial(PType.UNMAPPED_VAL);
	}

	@Override
	public void visit(SimpleParameter param) throws FlowAstException  {
		Part root = param.getRoot();
		switch (root.getType()) {

		case NORMAL: {
			final ParamNum paramNum = searchParamWithName(root.name);

			if (method.isStatic() && paramNum.isThis()) {
				throw new SemanticException("Reference to this pointer of a static method: " + method, currentLine);
			} else if (method.isVoid() && paramNum.isResult()) {
				throw new SemanticException("Reference to result value of void method '" + root.name + "' of method " + method, currentLine);
			} else if (!paramNum.isUnmapped()) {
				param.setMappedTo(paramNum);
			} else if (!root.name.matches("[a-zA-Z][a-zA-Z0-9_]*")) {
				throw new SemanticException("Reference to unknown parameter '" + root.name + "' of method " + method, currentLine);
			} else {
				// maybe a static field - leave it unmapped
			}
		} break;

		case EXC: {
			param.setMappedTo(ParamNum.createSpecial(PType.EXCEPTION_VAL));
		} break;

		case RESULT: {
			if (method.isVoid()) {
				throw new SemanticException("Reference to result value of void method '" + root.name + "' of method " + method, currentLine);
			}
			param.setMappedTo(ParamNum.createSpecial(PType.RESULT_VAL));
		} break;

		case STATE: {
			param.setMappedTo(ParamNum.createSpecial(PType.STATE_VAL));
			if (param.getParts().size() > 1) {
				throw new SemanticException("A wildcard parameter may not reference fields: " + root.getType()
						+ "('" + root.name + "') of method " + method, currentLine);
			}
		} break;

		case WILDCARD: {
			param.setMappedTo(ParamNum.createSpecial(PType.ALL_VAL));
		} break;
		case ARRAY:
		default: {
			throw new SemanticException("Unexpected parameter root: " + root.getType() + "('" + root.name + "') of method " + method, currentLine);
		}
		}
	}

	@Override
	public void visit(IFCStmt ifc) throws FlowAstException  {
		currentLine = ifc.getLineNr();

		if (ifc.hasAliasStmt()) {
			ifc.getAliasStmt().accept(this);
		}

		if (ifc.hasFlowStmts()) {
			for (FlowStmt flow : ifc.getFlowStmts()) {
				flow.accept(this);
			}
		}

		currentLine = 0;
	}

	@Override
	public void visit(ExplicitFlowStmt ifc) throws FlowAstException  {
		for (SimpleParameter p : ifc.getFrom()) {
			p.accept(this);
		}

		for (SimpleParameter p : ifc.getTo()) {
			p.accept(this);
		}
	}

	@Override
	public void visit(ParameterOptList param) throws FlowAstException {
		for (SimpleParameter p : param.getParams()) {
			p.accept(this);
		}
	}

	@Override
	public void visit(UniqueStmt unique) throws FlowAstException {
		for (Parameter p : unique.getParams()) {
			p.accept(this);
		}
	}

	@Override
	public void visit(PureStmt pure) throws FlowAstException {
		for (Parameter p : pure.getParams()) {
			p.accept(this);
		}
	}

	@Override
	public void visit(InferableAliasStmt alias) throws FlowAstException {
		// nothing to check
	}

}
