/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;

import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.IR;

/**
 * Used to show assignments of constant values to a variable.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ConstantPhiValueNode extends AbstractPDGNode {

	private final int valueNum;

	/**
	 * Creates a new node.
	 * @param id
	 * @param valueNum This is the value number of the constant value.
	 */
	ConstantPhiValueNode(int id, int valueNum) {
		super(id);
		this.valueNum = valueNum;
	}

	@Override
	public void accept(IPDGNodeVisitor visitor) {
		visitor.visitConstPhiValue(this);
	}

	public int getValueNum() {
		return valueNum;
	}

	public ConstantValue getConstant(IR ir) {
		return (ConstantValue) ir.getSymbolTable().getValue(valueNum);
	}

}
