/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class EntryNode extends AbstractPDGNode {

	EntryNode(int id) {
		super(id);
	}

	public void accept(IPDGNodeVisitor visitor) {
		visitor.visitEntry(this);
	}

}
