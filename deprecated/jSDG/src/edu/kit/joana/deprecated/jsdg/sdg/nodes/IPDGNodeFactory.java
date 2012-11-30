/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public interface IPDGNodeFactory {

	public CallNode makeCall(SSAInvokeInstruction instr, CGNode target);
	public CallNode makeCallDummy(SSAInvokeInstruction instr);
	public CallNode makeStaticRootCall(IMethod method, CGNode target);
	public EntryNode makeEntry(String methodSig);
	public NormalNode makeCompound(SSAInvokeInstruction instr);

	public ExpressionNode makeExpression(SSAInstruction instr);

	public PhiValueNode makePhiValue(SSAPhiInstruction instr);
	public ConstantPhiValueNode makeConstantPhiValue(int valueNum, IR ir);

	public NormalNode makeNormal(SSAInstruction instr);
	public PredicateNode makePredicate(SSAInstruction instr);
	public CatchNode makeCatch(SSAInstruction instr, int basicBlock, int val);
	public SyncNode makeSync(SSAMonitorInstruction instr);

}
