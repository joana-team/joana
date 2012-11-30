/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg;

import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.ir.ssa.AstEchoInstruction;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction;
import com.ibm.wala.cast.java.ssa.AstJavaInstructionVisitor;
import com.ibm.wala.cast.java.ssa.AstJavaInvokeInstruction;
import com.ibm.wala.cast.java.ssa.EnclosingObjectReference;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstruction.IVisitor;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CatchNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ExpressionNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.FieldGetArrayNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.FieldSetArrayNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.NormalNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterFieldFactory;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.PhiValueNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.PredicateNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.SyncNode;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.deprecated.jsdg.wala.SourceLocation;
import edu.kit.joana.wala.util.NotImplementedException;

/**
 * Creates PDG nodes from the Wala intermediate representation.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class PDGNodeCreationVisitor implements IVisitor, AstJavaInstructionVisitor {

	private final IntermediatePDG pdg;
	private final boolean ignoreExceptions;
	private final Map<AbstractPDGNode, SourceLocation> locations;
	private final IBytecodeMethod im;

	public PDGNodeCreationVisitor(IntermediatePDG pdg,
			Map<AbstractPDGNode, SourceLocation> locations, boolean ignoreExceptions) {
		this.pdg = pdg;
		this.locations = locations;
		this.ignoreExceptions = ignoreExceptions;
		IMethod m = pdg.getMethod();
		if (m instanceof IBytecodeMethod) {
			this.im = (IBytecodeMethod) m;
		} else if (m instanceof SummarizedMethod) {
			this.im = null;
		} else {
			throw new IllegalStateException("Can not handle non-bytecode methods");
		}
	}

	private void addLocationToMap(SSAInstruction instr, AbstractPDGNode node) {
		final Integer instrIndex = pdg.getSSAIndex(instr);

		try {
			if (im != null) {
				final int bcIndex = im.getBytecodeIndex(instrIndex);
				SourcePosition pos = im.getSourcePosition(bcIndex);
				SourceLocation loc = SourceLocation.getLocation(pdg.getSourceFileName(), pos.getFirstLine(),
						(pos.getFirstCol() >= 0 ? pos.getFirstCol() : 0), pos.getLastLine(),
						(pos.getLastCol() >= 0 ? pos.getLastCol() : 0));

				locations.put(node, loc);
			}
		} catch (InvalidClassFileException e) {
			Log.warn("No location for instruction " + instr + "\nfor " + pdg);
			Log.error(e);
		} catch (NullPointerException e) {
			Log.info("No location for instruction " + instr + "\nfor " + pdg);
		} catch (ArrayIndexOutOfBoundsException e) {
			Log.warn("No location for instruction " + instr + "\nfor " + pdg);
			Log.error(e);
		}
	}

	public void visitArrayLength(SSAArrayLengthInstruction instr) {
		int dest = instr.getDef();

		ExpressionNode node = pdg.makeExpression(instr);

		node.setLabel(tmpName(dest) + " = " + tmpName(instr.getArrayRef()) + ".length");

		pdg.addDefinesVar(node, dest);
		pdg.addDataFlow(node, instr);

		pdg.addArrayLength(instr);

		addLocationToMap(instr, node);
	}

	public void visitArrayLoad(SSAArrayLoadInstruction instr) {
		int dest = instr.getDef();

		TypeReference elemType = instr.getElementType();
		ParameterField afield = ParameterFieldFactory.getFactory().getArrayField(elemType);
		FieldGetArrayNode node = pdg.makeFieldGetArray(instr, afield);

		node.setLabel(tmpName(dest) + " = " + tmpName(instr.getArrayRef())
				+ "[" + tmpName(instr.getIndex()) + "]");

		pdg.addDefinesVar(node, dest);
//		pdg.addDataFlow(node, instr); // this should add index and arrayref automatically, but it doesnt hurt to add them again just to be sure

		pdg.addDataFlow(node.getIndexValue(), instr.getIndex());
		pdg.addDataFlow(node.getBaseValue(), instr.getArrayRef());

		pdg.addGet(instr);

		addLocationToMap(instr, node);
	}

	public void visitArrayStore(SSAArrayStoreInstruction instr) {
		int dest = instr.getArrayRef();

		TypeReference elemType = instr.getElementType();
		ParameterField afield = ParameterFieldFactory.getFactory().getArrayField(elemType);
		FieldSetArrayNode node = pdg.makeFieldSetArray(instr, afield);

		node.setLabel(tmpName(dest) + "[" +	tmpName(instr.getIndex()) + "] = "
				+ tmpName(instr.getValue()));

		pdg.addDataFlow(node, dest);
		pdg.addDataFlow(node.getIndexValue(), instr.getIndex());
		pdg.addDataFlow(node.getSetValue(), instr.getValue());

		pdg.addSet(instr);

		addLocationToMap(instr, node);
	}

	public void visitBinaryOp(SSABinaryOpInstruction instr) {
		int dest = instr.getDef();

		ExpressionNode node = pdg.makeExpression(instr);
		node.setType(ExpressionNode.Type.BINARY);

		String ops = tmpName(instr.getUse(0));
		for (int j = 1; j < instr.getNumberOfUses(); j++) {
			ops += ", " + tmpName(instr.getUse(j));
		}
		node.setLabel(tmpName(dest) + " = OPER " + instr.getOperator() + "("
				+ ops + ")");

		pdg.addDefinesVar(node, dest);
		pdg.addDataFlow(node, instr);

		addLocationToMap(instr, node);
	}

	public void visitCheckCast(SSACheckCastInstruction instr) {
		int dest = instr.getResult();

		ExpressionNode node = pdg.makeExpression(instr);
		node.setType(ExpressionNode.Type.CHECKCAST);

		node.setLabel(tmpName(dest) + " = CHECKCAST "
				+ tmpName(instr.getVal()));

		pdg.addDefinesVar(node, dest);
		pdg.addDataFlow(node, instr.getVal());

		addLocationToMap(instr, node);
	}

	public void visitComparison(SSAComparisonInstruction instr) {
		// comparisons between long, float and double values
		int dest = instr.getDef();

		ExpressionNode node = pdg.makeExpression(instr);
		node.setType(ExpressionNode.Type.COMPARE);

		String ops = tmpName(instr.getUse(0));
		for (int j = 1; j < instr.getNumberOfUses(); j++) {
			ops += ", " + tmpName(instr.getUse(j));
		}
		node.setLabel(tmpName(dest) + " = CMP " + Util.opcode2String(instr.getOperator()) + "(" + ops + ")");

		pdg.addDefinesVar(node, dest);
		pdg.addDataFlow(node, instr);

		addLocationToMap(instr, node);
	}

	public void visitConditionalBranch(
			SSAConditionalBranchInstruction instr) {
		PredicateNode node = pdg.makePredicate(instr);
		node.setLabel("if (" + tmpName(instr.getUse(0)) + " " + Util.operator2String(instr.getOperator()) + " "
				+ tmpName(instr.getUse(1)) + ")");

		pdg.addDataFlow(node, instr);

		addLocationToMap(instr, node);
	}

	public void visitConversion(SSAConversionInstruction instr) {
		int dest = instr.getDef();

		ExpressionNode node = pdg.makeExpression(instr);
		node.setType(ExpressionNode.Type.CONVERT);
		node.setLabel(tmpName(dest) + " = CONVERT " + instr.getFromType().getName() + " to "
				+ instr.getToType().getName() + " " + tmpName(instr.getUse(0)));

		pdg.addDefinesVar(node, dest);
		pdg.addDataFlow(node, instr);

		addLocationToMap(instr, node);
	}

	public void visitGet(SSAGetInstruction instr) {
		final int dest = instr.getDef();
		final int ref = instr.getRef();

		FieldReference fRef = instr.getDeclaredField();
		IField ifield = pdg.getHierarchy().resolveField(fRef);
		ParameterField field = null;
		if (ifield == null) {
			Log.warn("Could not resolve field " + fRef + " - " + instr);
		} else {
			field = ParameterFieldFactory.getFactory().getObjectField(ifield);
		}

		ExpressionNode node = null;

		String rvalue;
		if (instr.isStatic()) {
			/**
			 * As wala ignores some classes during the analysis
			 * (see jSDGExclusions.txt) there may be fields that cannot be
			 * resolved. So we have to ignore them in our analysis.
			 */
			if (field != null) {
				node = pdg.makeFieldGetStatic(instr, field);

				pdg.addGet(instr);

				String fieldName = Util.fieldName(field);
				rvalue = fieldName;
			} else {
				node = pdg.makeExpression(instr);

				rvalue = fRef.getSignature();
			}
		} else {
			if (field == null) {
				node = pdg.makeExpression(instr);
				rvalue = fRef.getSignature();
			} else {
				node = pdg.makeFieldGet(instr, field);
				rvalue = tmpName(ref) + "." + fRef.getName().toString();
				pdg.addDataFlow(node.getBaseValue(), ref);
				pdg.addGet(instr);
			}
		}

		node.setLabel(tmpName(dest) + " = " + rvalue);

		pdg.addDefinesVar(node, dest);

		addLocationToMap(instr, node);
	}

	public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instr) {
		if (!ignoreExceptions) {
			int dest = instr.getDef();

			int bbnum = instr.getBasicBlockNumber();

			CatchNode node = pdg.makeCatch(instr, bbnum, dest);
			pdg.addDefinesVar(node, dest);

			addLocationToMap(instr, node);
		}
	}

	public void visitGoto(SSAGotoInstruction instr) {
		// nothing to do here
		// goto only affects control flow, which is already covered by the cfg
	}

	public void visitInstanceof(SSAInstanceofInstruction instr) {
		int dest = instr.getDef();
		int ref = instr.getRef();

		ExpressionNode node = pdg.makeExpression(instr);

		TypeName tName = instr.getCheckedType().getName();
		node.setLabel(tmpName(dest) + "=" + tmpName(ref) +
			" INSTANCEOF " + Util.typeName(tName));

		pdg.addDefinesVar(node, dest);
		pdg.addDataFlow(node, ref);

		addLocationToMap(instr, node);
	}

	public void visitInvoke(SSAInvokeInstruction instr) {
		CallGraph cg = pdg.getCallGraph();
		Set<CGNode> targets =
			cg.getPossibleTargets(pdg.getCallGraphNode(), instr.getCallSite());

		AbstractPDGNode mainNode = null;
		if (targets.isEmpty()) {
			Log.error("No resolved calls for " + Util.methodName(instr.getDeclaredTarget()));
			Log.error("Check if buildpath is correct and includes a matching method.");

			CallNode callDummy = pdg.makeCallDummy(instr);
			pdg.addCall(callDummy);
			addLocationToMap(instr, callDummy);
		} else if (targets.size() > 1) {
			// create artificial main node for calls with multiple targets...
			mainNode = pdg.makeCompound(instr);
			addLocationToMap(instr, mainNode);
		}

		for (CGNode target : targets) {
			CallNode call = pdg.makeCall(instr, target);

			if (mainNode != null) {
				pdg.addUnconditionalControlDependency(mainNode, call);
			}

			pdg.addCall(call);

			addLocationToMap(instr, call);
		}
	}

	public static final boolean CHECK_MONITOR_BALANCED = false;
	private Stack<Integer> monitors = (CHECK_MONITOR_BALANCED ? new Stack<Integer>() : (Stack<Integer>) null);

	public void visitMonitor(SSAMonitorInstruction instr) {
		int ref = instr.getRef();

		SyncNode node = pdg.makeSync(instr);
		node.setLabel((instr.isMonitorEnter() ? "MONITORENTER " : "MONITOREXIT ") + tmpName(ref));

		pdg.addDataFlow(node, ref);

		addLocationToMap(instr, node);

		if (CHECK_MONITOR_BALANCED) {
			if (instr.isMonitorEnter()) {
				monitors.push(ref);
			} else {
				if (monitors.size() > 0) {
					int lock = monitors.pop();
					if (lock != ref) {
						// unmatched monitor in stack!
//						System.err.println("Unbalanced monitorexit v" + ref + " - active monitorenter is v" + lock );
						// push lock back onto stack to hopefully prevent further errors msgs
						monitors.push(lock);
					} else {
						System.err.println("MATCH OK: monitorenter-exit v" + ref);
					}
				} else {
//					System.err.println("Unbalanced monitorexit v" + ref + " - no active monitorenter found." );
				}
			}
		}
	}

	public void visitNew(SSANewInstruction instr) {
		int dest = instr.getDef();

		ExpressionNode node = pdg.makeExpression(instr);
		TypeReference tRef = instr.getConcreteType();
		IClass iclass = pdg.getHierarchy().lookupClass(tRef);
		if (iclass != null) {
			node.setLabel(tmpName(dest) + " = new " +
					Util.typeName(iclass.getName()));
		} else {
			node.setLabel(tmpName(dest) + " = new " + Util.typeName(tRef.getName()));
		}

		pdg.addDefinesVar(node, dest);

		addLocationToMap(instr, node);
	}

	public void visitPhi(SSAPhiInstruction instr) {
		int dest = instr.getDef();

		PhiValueNode node = pdg.makePhiValue(instr);
		pdg.addDefinesVar(node, dest);

		for (int i = 0; i < instr.getNumberOfUses(); i++) {
			int use = instr.getUse(i);
			pdg.addDataFlow(node, use);
		}

		addLocationToMap(instr, node);
	}

	public void visitPi(SSAPiInstruction instr) {
		throw new NotImplementedException();
	}

	public void visitPut(SSAPutInstruction instr) {
		final int src = instr.getVal();

		IField ifield = pdg.getHierarchy().resolveField(instr.getDeclaredField());
		String lvalue;
		ParameterField field = null;
		if (ifield != null) {
			field = ParameterFieldFactory.getFactory().getObjectField(ifield);
		}

		ExpressionNode expr = null;
		if (instr.isStatic()) {
			if (field == null) {
				/**
				 * As wala ignores some classes during the analysis
				 * (see jSDGExclusions.txt) there may be fields that cannot be
				 * resolved. So we have to ignore them in our analysis.
				 */
				Log.warn("Could not resolve field " + instr.getDeclaredField() + " of type " + instr.getDeclaredFieldType());
				expr = pdg.makeExpression(instr);
				lvalue = instr.getDeclaredField().getName().toString();

				pdg.addDataFlow(expr, src);
			} else {
				expr = pdg.makeFieldSetStatic(instr, field);

				lvalue = Util.fieldName(field);

				pdg.addSet(instr);
				pdg.addDataFlow(expr.getSetValue(), src);
			}
		} else {
			final int ref = instr.getRef();

			if (field == null) {
				lvalue = tmpName(ref) + "." + instr.getDeclaredField().getName();
				expr = pdg.makeExpression(instr);
				pdg.addDataFlow(expr, src);
			} else {
				expr = pdg.makeFieldSet(instr, field);
				lvalue = tmpName(ref) + "." + field.getName();
				pdg.addDataFlow(expr.getBaseValue(), ref);

				pdg.addSet(instr);

				pdg.addDataFlow(expr.getSetValue(), src);
			}
		}

		expr.setLabel(lvalue + " = " + tmpName(src));

		addLocationToMap(instr, expr);
	}

	public void visitReturn(SSAReturnInstruction instr) {
		NormalNode node = pdg.makeNormal(instr);

		if (!instr.returnsVoid()) {
			int ret = instr.getResult();

			node.setLabel("return " + tmpName(ret));
			pdg.addDataFlow(node, ret);
			pdg.addReturnTmp(instr, ret);
		} else {
			node.setLabel("return");
		}

		pdg.addReturn(node);

		addLocationToMap(instr, node);
	}

	public void visitSwitch(SSASwitchInstruction instr) {
		int var = instr.getUse(0);

		PredicateNode node = pdg.makePredicate(instr);
		node.setLabel("switch " + tmpName(var));

		pdg.addDataFlow(node, var);

		addLocationToMap(instr, node);
	}

	public void visitThrow(SSAThrowInstruction instr) {
		if (!ignoreExceptions) {
			int val = instr.getException();

			NormalNode node = pdg.makeNormal(instr);
			node.setLabel("throw " + tmpName(val));

			pdg.addThrow(node);
			pdg.addThrowTmp(instr, val);

			addLocationToMap(instr, node);
		}
	}

	public void visitUnaryOp(SSAUnaryOpInstruction instr) {
		int dest = instr.getDef();
		int oper = instr.getUse(0);

		ExpressionNode node = pdg.makeExpression(instr);
		node.setLabel(tmpName(dest) + " = OPER " +
				Util.operator2String(instr.getOpcode()) + "(" +
				tmpName(oper) + ")");

		pdg.addDefinesVar(node, dest);
		pdg.addDataFlow(node, oper);

		addLocationToMap(instr, node);
	}

	private String tmpName(int var) {
		return Util.tmpName(pdg, var);
	}

	/*
	 * the not implemented methods below have to be there since we want also to
	 * be able to visit AST created by CAst and not only Shrike style ASTs
	 */

	public void visitEnclosingObjectReference(EnclosingObjectReference inst) {
		Log.warn("implement visitEnclosingObjectReference");
	}

	public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {
		Log.warn("implement visitJavaInvoke");
	}

	public void visitAssert(AstAssertInstruction instruction) {
		Log.warn("implement visitAssert");
	}

	public void visitAstGlobalRead(AstGlobalRead instruction) {
		Log.warn("implement visitAstGlobalRead");
	}

	public void visitAstGlobalWrite(AstGlobalWrite instruction) {
		Log.warn("implement visitAstGlobalWrite");
	}

	public void visitAstLexicalRead(AstLexicalRead instruction) {
		Log.warn("implement visitAstLexicalRead");
	}

	public void visitAstLexicalWrite(AstLexicalWrite instruction) {
		Log.warn("implement visitAstLexicalWrite");
	}

	public void visitEachElementGet(EachElementGetInstruction inst) {
		Log.warn("implement visitEachElementGet");
	}

	public void visitEachElementHasNext(EachElementHasNextInstruction inst) {
		Log.warn("implement visitEachElementHasNext");
	}

	public void visitEcho(AstEchoInstruction inst) {
		Log.warn("implement visitEcho");
	}

	public void visitIsDefined(AstIsDefinedInstruction inst) {
		Log.warn("implement visitIsDefined");
	}

	public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
		Log.warn("implement visitLoadMetadata");
	}

	public void done() {
		if (CHECK_MONITOR_BALANCED) {
			for (Integer lock : monitors) {
				System.err.println("Unbalanced monitorenter v" + lock + " - no matching monitorexit found.");
			}
		}
	}

}
