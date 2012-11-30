/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;


/**
 * A node for PhiValues
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class PhiValueNode extends AbstractPDGNode {

	private final int defVar;
	private final int[] refVar;

	PhiValueNode(int id, int defVar, int[] refVar) {
		super(id);
		this.defVar = defVar;
		this.refVar = refVar;
	}

	@Override
	public void accept(IPDGNodeVisitor visitor) {
		visitor.visitPhiValue(this);
	}

	public int getDef() {
		return defVar;
	}

	public int[] getRef() {
		return refVar;
	}

}
