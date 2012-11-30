/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.MethodReference;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class CallNode extends AbstractPDGNode {

	private final SSAInvokeInstruction instr;
	private final CGNode target;

	/**
	 * Constructor for static root calls - as static initializers.
	 */
	CallNode(int id, CGNode target) {
		super(id);

		assert (target != null);

		this.instr = null;
		this.target = target;
	}

	CallNode(int id, SSAInvokeInstruction instr, CGNode target) {
		super(id);

		assert (instr != null);
		assert (target != null);

		this.instr = instr;
		this.target = target;
	}

	CallNode(int id, SSAInvokeInstruction instr) {
		super(id);

		assert (instr != null);

		this.instr = instr;
		this.target = null;
	}

	public void accept(IPDGNodeVisitor visitor) {
		visitor.visitCall(this);
	}

	public boolean isStaticRootCall() {
		return instr == null;
	}

	public CallSiteReference getCallSite() {
		return (instr != null ? instr.getCallSite() : null);
	}

	public MethodReference getReferencedTarget() {
		return (instr != null ? instr.getDeclaredTarget() :
			target.getMethod().getReference());
	}

	public CGNode getTarget() {
		return target;
	}

	public SSAInvokeInstruction getInstruction() {
		return instr;
	}

	public boolean isDummy() {
		return target == null;
	}
}
