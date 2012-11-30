/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree;

import java.util.Set;

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
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
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
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterFieldFactory;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;

/**
 * Creates basic nodes from instruction: Static fields, Root act-in/-out
 * for method calls.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ObjTreeBasicNodeCreationVisitor implements IVisitor,
		AstJavaInstructionVisitor {

	private final PDG pdg;
	private final ObjTreeParamModel model;

	public ObjTreeBasicNodeCreationVisitor(PDG pdg, ObjTreeParamModel model) {
		this.pdg = pdg;
		this.model = model;
	}

	public void visitGet(SSAGetInstruction instr) {
		if (instr.isStatic()) {
			FieldReference fRef = instr.getDeclaredField();
			IField ifield = pdg.getHierarchy().resolveField(fRef);
			ParameterField field = null;
			if (ifield == null) {
				Log.warn("Could not resolve field " + fRef + " - " + instr);
			} else {
				field = ParameterFieldFactory.getFactory().getObjectField(ifield);
			}

			/**
			 * As wala ignores some classes during the analysis
			 * (see jSDGExclusions.txt) there may be fields that cannot be
			 * resolved. So we have to ignore them in our analysis.
			 */
			if (field != null) {
				String fieldName = Util.fieldName(field);

				FormInOutNode staticNode = model.getStaticFormIn(field);
				if (staticNode == null) {
					if (!field.isPrimitiveType()) {
						int dest = instr.getDef();
						OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(dest);
						staticNode = model.makeFormInOutStatic(true, field, pts);
					} else {
						staticNode = model.makeFormInOutStaticPrimitive(true, field);
					}
					staticNode.setLabel(fieldName);

					pdg.addParameterChildDependency(pdg.getRoot(), staticNode);
					model.addStaticFormIn(staticNode);
				}
			}
		}
	}

	public void visitPut(SSAPutInstruction instr) {
		if (instr.isStatic()) {
			FieldReference fRef = instr.getDeclaredField();
			IField ifield = pdg.getHierarchy().resolveField(fRef);
			ParameterField field = null;
			if (ifield == null) {
				Log.warn("Could not resolve field " + fRef + " - " + instr);
			} else {
				field = ParameterFieldFactory.getFactory().getObjectField(ifield);
			}

			if (field != null) {
				String fieldName = Util.fieldName(field);

				FormInOutNode formIn = model.getStaticFormIn(field);

				if (formIn == null) {
					if (!field.isPrimitiveType()) {
						OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(field);
						formIn = model.makeFormInOutStatic(true, field, pts);
					} else {
						formIn = model.makeFormInOutStaticPrimitive(true, field);
					}
					formIn.setLabel(fieldName);
					pdg.addParameterChildDependency(pdg.getRoot(), formIn);
					model.addStaticFormIn(formIn);
				}

				FormInOutNode formOut = model.getStaticFormOut(field);
				if (formOut == null) {
					if (!field.isPrimitiveType()) {
						OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(field);
						formOut = model.makeFormInOutStatic(false, field, pts);
					} else {
						formOut = model.makeFormInOutStaticPrimitive(false, field);
					}
					formOut.setLabel(fieldName);
					pdg.addParameterChildDependency(pdg.getRoot(), formOut);
					model.addStaticFormOut(formOut);
				}
			}
		}
	}

	public void visitInvoke(SSAInvokeInstruction instr) {
		Set<CallNode> nodes = pdg.getCallsForInstruction(instr);
		if (nodes == null || nodes.size() == 0) {
			Log.warn("No node found for instruction: " + instr);
			return;
		}

 		for (CallNode call : nodes) {
			// handle the this pointer
			if (!instr.isStatic()) {
				final int thisParamId = 0;
				final int thisParam = instr.getUse(thisParamId);

				OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(thisParam);
				ActualInOutNode pnode =
					model.makeActualInOut(true, instr, thisParam, thisParamId, pts);
				pnode.setLabel("this");

				pdg.addDataFlow(pnode, thisParam);
				pdg.addParameterChildDependency(call, pnode);

				model.addRootActIn(call, pnode);
				// we got the this-pointer param at hand now
			    // virtual calls are dependent on the type of the target object
				pdg.addVirtualDependency(pnode, call);
			}

			MethodReference mRef = instr.getDeclaredTarget();
			// start at parameter 0 for static methods and at 1 for non-static ones
			// this way we ignore the this pointer
			final int startAt = 0;
			final int stopAt = mRef.getNumberOfParameters();
			final int thisPointerOffset = (instr.isStatic() ? 0 : 1);

			// the this pointer is treated separately
			for (int paramId = startAt; paramId < stopAt; paramId++) {
				final int idInclThis = paramId + thisPointerOffset;
				final int param = instr.getUse(idInclThis);

				ActualInOutNode pnode;
				if (mRef.getParameterType(paramId).isPrimitiveType()) {
					pnode = model.makeActualInPrimitive(instr, param, idInclThis);

					if (pdg.getIR().getSymbolTable().isConstant(param)) {
						Object constant = pdg.getIR().getSymbolTable().getConstantValue(param);
						String label = Util.sanitizeLabel(constant);
						pnode.setLabel("const " + label);
					} else {
						pnode.setLabel("primitive param " + idInclThis);
					}
				} else if (pdg.getIR().getSymbolTable().isConstant(param)) {
					OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(param);
					pnode = model.makeActualInConstant(instr, param, idInclThis, pts);

					Object constant = pdg.getIR().getSymbolTable().getConstantValue(param);
					String label = Util.sanitizeLabel(constant);
					pnode.setLabel("const " + label);
				} else {
					OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(param);
					pnode = model.makeActualInOut(true, instr, param, idInclThis, pts);
					pnode.setLabel("param " + idInclThis);
				}

				pdg.addDataFlow(pnode, param);
				pdg.addParameterChildDependency(call, pnode);

				model.addRootActIn(call, pnode);
			}

			if (instr.hasDef()) {
				int dest = instr.getDef();

				ActualInOutNode retVal;
				if (instr.getDeclaredResultType().isPrimitiveType()) {
					retVal = model.makeActualOutReturnPrimitive(dest, instr);
				} else {
					OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(dest);
					retVal = model.makeActualOutReturn(dest, instr, pts);
				}

				retVal.setLabel(Util.methodName(instr.getDeclaredTarget()));

				pdg.addDefinesVar(retVal, dest);
				pdg.addParameterChildDependency(call, retVal);
				model.addRootActOut(call, retVal);
			}

			if (!pdg.isIgnoreExceptions()) {
				int ssaExc = instr.getException();

				OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(ssaExc);
				ActualInOutNode retExc =
					model.makeActualOutException(ssaExc, instr, pts);
				retExc.setLabel("exception " + Util.tmpName(pdg, ssaExc));

				pdg.addDefinesVar(retExc, ssaExc);
				pdg.addParameterChildDependency(call, retExc);
				model.addRootActOut(call, retExc);
			}

 		}
	}


	public void visitArrayLength(SSAArrayLengthInstruction instruction) {}
	public void visitArrayLoad(SSAArrayLoadInstruction instruction) {}
	public void visitArrayStore(SSAArrayStoreInstruction instruction) {}
	public void visitBinaryOp(SSABinaryOpInstruction instruction) {}
	public void visitCheckCast(SSACheckCastInstruction instruction) {}
	public void visitComparison(SSAComparisonInstruction instruction) {}
	public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {}
	public void visitConversion(SSAConversionInstruction instruction) {}
	public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {}
	public void visitGoto(SSAGotoInstruction instruction) {}
	public void visitInstanceof(SSAInstanceofInstruction instruction) {}
	public void visitMonitor(SSAMonitorInstruction instruction) {}
	public void visitNew(SSANewInstruction instruction) {}
	public void visitPhi(SSAPhiInstruction instruction) {}
	public void visitPi(SSAPiInstruction instruction) {}
	public void visitReturn(SSAReturnInstruction instruction) {}
	public void visitSwitch(SSASwitchInstruction instruction) {}
	public void visitThrow(SSAThrowInstruction instruction) {}
	public void visitUnaryOp(SSAUnaryOpInstruction instruction) {}
	public void visitEnclosingObjectReference(EnclosingObjectReference inst) {}
	public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {}
	public void visitAssert(AstAssertInstruction instruction) {}
	public void visitAstGlobalRead(AstGlobalRead instruction) {}
	public void visitAstGlobalWrite(AstGlobalWrite instruction) {}
	public void visitAstLexicalRead(AstLexicalRead instruction) {}
	public void visitAstLexicalWrite(AstLexicalWrite instruction) {}
	public void visitEachElementGet(EachElementGetInstruction inst) {}
	public void visitEachElementHasNext(EachElementHasNextInstruction inst) {}
	public void visitEcho(AstEchoInstruction inst) {}
	public void visitIsDefined(AstIsDefinedInstruction inst) {}
	public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {}
}
