/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph;

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
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
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
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterFieldFactory;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ActInLocalNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ActInNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ActOutExceptionNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ActOutLocalNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ActOutNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormInHeapNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.FormOutHeapNode;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.deprecated.jsdg.wala.BytecodeLocation;

/**
 * Visitor for the statements of the intermediate representation.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ObjGraphBasicNodeCreationVisitor implements IVisitor,
		AstJavaInstructionVisitor {

	private final PDG pdg;
	private final ObjGraphParamModel model;

	public ObjGraphBasicNodeCreationVisitor(PDG pdg, ObjGraphParamModel model) {
		this.pdg = pdg;
		this.model = model;
	}

	public void visitGet(SSAGetInstruction instr) {
		FieldReference fRef = instr.getDeclaredField();
		IField ifield = pdg.getHierarchy().resolveField(fRef);
		if (ifield == null) {
			Log.warn("Could not resolve field " + fRef + " - " + instr);
		} else {
			ParameterField field = ParameterFieldFactory.getFactory().getObjectField(ifield);
			/**
			 * As wala ignores some classes during the analysis
			 * (see jSDGExclusions.txt) there may be fields that cannot be
			 * resolved. So we have to ignore them in our analysis.
			 */

			String fieldName = Util.fieldName(field);
			FormInHeapNode fIn = null;

			if (instr.isStatic()) {
				PointerKey pk = pdg.getPointerKey(field);
				Set<PointerKey> pKeys = HashSetFactory.make();
				pKeys.add(pk);
				OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(field);
				fIn = model.makeFormInHeap(field, field.isPrimitiveType(), ifield.getFieldTypeReference(), pKeys, pts);
			} else {
				PointerKey pk = pdg.getPointerKey(instr.getDef());
				Set<PointerKey> pKeys = HashSetFactory.make();
				pKeys.add(pk);
				OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(instr.getDef());
				OrdinalSet<InstanceKey> basePts = pdg.getPointsToSet(instr.getRef());
				fIn = model.makeFormInHeap(basePts, field, field.isPrimitiveType(), ifield.getFieldTypeReference(), pKeys, pts);
			}

			fIn.setLabel(fieldName);

			pdg.addParameterChildDependency(pdg.getRoot(), fIn);
			model.addRef(fIn);
		}
	}

	public void visitPut(SSAPutInstruction instr) {
		FieldReference fRef = instr.getDeclaredField();
		IField ifield = pdg.getHierarchy().resolveField(fRef);

		if (ifield == null) {
			Log.warn("Could not resolve field " + fRef + " - " + instr);
		} else {
			ParameterField field = ParameterFieldFactory.getFactory().getObjectField(ifield);
			FormOutHeapNode formOut = null;

			if (instr.isStatic()) {
				PointerKey pk = pdg.getPointerKey(field);
				Set<PointerKey> pKeys = HashSetFactory.make();
				pKeys.add(pk);
				OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(field);
				formOut = model.makeFormOutHeap(field, field.isPrimitiveType(), ifield.getFieldTypeReference(), pKeys, pts);
			} else {
				PointerKey pk = pdg.getPointerKey(instr.getVal());
				Set<PointerKey> pKeys = HashSetFactory.make();
				pKeys.add(pk);
				OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(instr.getVal());
				OrdinalSet<InstanceKey> basePts = pdg.getPointsToSet(instr.getRef());
				formOut = model.makeFormOutHeap(basePts, field, field.isPrimitiveType(), ifield.getFieldTypeReference(), pKeys, pts);
			}

			String fieldName = Util.fieldName(field);
			formOut.setLabel(fieldName);
			pdg.addParameterChildDependency(pdg.getRoot(), formOut);

			model.addMod(formOut);
		}
	}

	public void visitInvoke(SSAInvokeInstruction instr) {
		Set<CallNode> nodes = pdg.getCallsForInstruction(instr);
		if (nodes == null || nodes.size() == 0) {
			Log.warn("No node found for instruction: " + instr);
			return;
		}

 		for (CallNode call : nodes) {

			MethodReference mRef = instr.getDeclaredTarget();

			final int stopAt = (instr.isStatic() ?
				mRef.getNumberOfParameters() :
				mRef.getNumberOfParameters() + 1);

			ActInLocalNode params[] = new ActInLocalNode[stopAt];
			for (int paramId = 0; paramId < stopAt; paramId++) {
				final int param = instr.getUse(paramId);
				final boolean isThis = paramId == 0 && !instr.isStatic();
				// this == 0, all normal params start at 1. So we have to add 1 to the static parameter numbers
				final int displayParamNum = (instr.isStatic() ? paramId + 1 : paramId);

				PointerKey pk = pdg.getPointerKey(param);
				Set<PointerKey> pKeys = HashSetFactory.make();
				pKeys.add(pk);

				OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(param);
				TypeReference tref;
				if (isThis) {
					tref = mRef.getDeclaringClass();
				} else {
					int id = (instr.isStatic() ? paramId : paramId - 1);
					tref = mRef.getParameterType(id);
				}

				ActInLocalNode aIn = model.makeActInLocal(tref.isPrimitiveType(), tref, pKeys, pts, call, paramId,
						displayParamNum);
				params[paramId] = aIn;

				if (pdg.getIR().getSymbolTable().isConstant(param)) {
					Object constant = pdg.getIR().getSymbolTable().getConstantValue(param);
					String label = Util.sanitizeLabel(constant);
					aIn.setLabel("param " + displayParamNum + " [const " + label + "]");
				} else if (isThis) {
					// we got the this-pointer param at hand now
				    // virtual calls are dependent on the type of the target object
					pdg.addVirtualDependency(aIn, call);

					aIn.setLabel("this");
				} else {
					aIn.setLabel("param " + displayParamNum);
				}

				pdg.addDataFlow(aIn, param);
				pdg.addParameterChildDependency(call, aIn);

				model.addRef(call, aIn);
			}

			model.setParameterIn(call, params);

			if (instr.hasDef()) {
				int dest = instr.getDef();

				PointerKey pk = pdg.getPointerKey(dest);
				Set<PointerKey> pKeys = HashSetFactory.make();
				pKeys.add(pk);

				OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(dest);
				TypeReference tref = instr.getDeclaredResultType();
				ActOutLocalNode retVal = model.makeActOutLocal(tref.isPrimitiveType(), tref, pKeys, pts, call,
						BytecodeLocation.UNDEFINED_POS_IN_BYTECODE, BytecodeLocation.UNDEFINED_POS_IN_BYTECODE);

				retVal.setLabel(Util.methodName(instr.getDeclaredTarget()));

				pdg.addDefinesVar(retVal, dest);
				pdg.addParameterChildDependency(call, retVal);

				model.addMod(call, retVal);

				model.setParameterReturn(call, retVal);
			}

			if (!pdg.isIgnoreExceptions()) {
				int ssaExc = instr.getException();
				if (ssaExc >= 0) {
					PointerKey pk = pdg.getPointerKey(ssaExc);
					Set<PointerKey> pKeys = HashSetFactory.make();
					pKeys.add(pk);

					OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(ssaExc);

					ActOutExceptionNode retExc = model.makeActOutException(pKeys, TypeReference.JavaLangException, pts, call);

					retExc.setLabel("exception " + Util.tmpName(pdg, ssaExc));

					pdg.addDefinesVar(retExc, ssaExc);
					pdg.addParameterChildDependency(call, retExc);

					model.addMod(call, retExc);
					model.setParameterException(call, retExc);
				}
			}

			if (call.isDummy()) {
				// add summary edges for call dummies
				ActOutNode ret = model.getParameterReturn(call);
				ActOutNode exc = model.getParameterException(call);
				if (ret == null && exc == null && !pdg.isIgnoreExceptions()) {
					exc = model.makeActOutLocal(true, TypeReference.JavaLangException, null, null, call,
							BytecodeLocation.UNDEFINED_POS_IN_BYTECODE, BytecodeLocation.UNDEFINED_POS_IN_BYTECODE);
					pdg.addParameterChildDependency(call, exc);
					pdg.addDataDependency(exc, pdg.getExceptionalExit());
					model.addMod(call, exc);
				}

				for (ActInNode aIn : model.getParameterIn(call)) {
					if (ret != null) {
						pdg.addSummaryEdge(aIn, ret);
					}
					if (exc != null) {
						pdg.addSummaryEdge(aIn, exc);
					}
				}
			}
 		}
	}

	public void visitArrayLoad(SSAArrayLoadInstruction instr) {
		TypeReference tRef = instr.getElementType();
		ParameterField aField = ParameterFieldFactory.getFactory().getArrayField(tRef);

		String fieldName = "[" + Util.typeName(tRef.getName()) + "]";
		FormInHeapNode fIn = null;

		PointerKey pk = pdg.getPointerKey(instr.getDef());
		Set<PointerKey> pKeys = HashSetFactory.make();
		pKeys.add(pk);

		OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(instr.getDef());
		OrdinalSet<InstanceKey> basePts = pdg.getPointsToSet(instr.getArrayRef());
		fIn = model.makeFormInHeap(basePts, aField, tRef.isPrimitiveType(), tRef, pKeys, pts);

		fIn.setLabel(fieldName);

		pdg.addParameterChildDependency(pdg.getRoot(), fIn);
		model.addRef(fIn);
	}

	public void visitArrayStore(SSAArrayStoreInstruction instr) {
		TypeReference tRef = instr.getElementType();
		ParameterField aField = ParameterFieldFactory.getFactory().getArrayField(tRef);

		String fieldName = "[" + Util.typeName(tRef.getName()) + "]";
		FormOutHeapNode fOut = null;

		PointerKey pk = pdg.getPointerKey(instr.getValue());
		Set<PointerKey> pKeys = HashSetFactory.make();
		pKeys.add(pk);

		OrdinalSet<InstanceKey> pts = pdg.getPointsToSet(instr.getValue());
		OrdinalSet<InstanceKey> basePts = pdg.getPointsToSet(instr.getArrayRef());
		fOut = model.makeFormOutHeap(basePts, aField, tRef.isPrimitiveType(), tRef, pKeys, pts);

		fOut.setLabel(fieldName);

		pdg.addParameterChildDependency(pdg.getRoot(), fOut);
		model.addMod(fOut);
	}

	public void visitArrayLength(SSAArrayLengthInstruction instruction) {}
	public void visitBinaryOp(SSABinaryOpInstruction instruction) {}
	public void visitCheckCast(SSACheckCastInstruction instruction) {}
	public void visitComparison(SSAComparisonInstruction instruction) {}
	public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {}
	public void visitConversion(SSAConversionInstruction instruction) {}
	public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instr) {}
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
