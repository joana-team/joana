/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

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

public class StdSSAInstructionVisitor implements IVisitor {

	@Override
	public void visitGoto(SSAGotoInstruction instruction) {
	}

	@Override
	public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
	}

	@Override
	public void visitArrayStore(SSAArrayStoreInstruction instruction) {
	}

	@Override
	public void visitBinaryOp(SSABinaryOpInstruction instruction) {
	}

	@Override
	public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
	}

	@Override
	public void visitConversion(SSAConversionInstruction instruction) {
	}

	@Override
	public void visitComparison(SSAComparisonInstruction instruction) {
	}

	@Override
	public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
	}

	@Override
	public void visitSwitch(SSASwitchInstruction instruction) {
	}

	@Override
	public void visitReturn(SSAReturnInstruction instruction) {
	}

	@Override
	public void visitGet(SSAGetInstruction instruction) {
	}

	@Override
	public void visitPut(SSAPutInstruction instruction) {
	}

	@Override
	public void visitInvoke(SSAInvokeInstruction instruction) {
	}

	@Override
	public void visitNew(SSANewInstruction instruction) {
	}

	@Override
	public void visitArrayLength(SSAArrayLengthInstruction instruction) {
	}

	@Override
	public void visitThrow(SSAThrowInstruction instruction) {
	}

	@Override
	public void visitMonitor(SSAMonitorInstruction instruction) {
	}

	@Override
	public void visitCheckCast(SSACheckCastInstruction instruction) {
	}

	@Override
	public void visitInstanceof(SSAInstanceofInstruction instruction) {
	}

	@Override
	public void visitPhi(SSAPhiInstruction instruction) {
	}

	@Override
	public void visitPi(SSAPiInstruction instruction) {
	}

	@Override
	public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
	}

	@Override
	public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
	}

}
