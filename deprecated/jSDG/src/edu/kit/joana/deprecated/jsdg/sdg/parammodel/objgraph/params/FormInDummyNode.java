/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params;


import com.ibm.wala.types.TypeReference;

import edu.kit.joana.deprecated.jsdg.wala.BytecodeLocation;

/**
 * Dummy formal-in nodes. Used for unused parameters.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class FormInDummyNode extends FormInLocalNode {

	public FormInDummyNode(int id, boolean isPrimitive, TypeReference type) {
		super(id, isPrimitive, type, null, null, BytecodeLocation.UNDEFINED_POS_IN_BYTECODE,
				BytecodeLocation.UNDEFINED_POS_IN_BYTECODE);
	}

}
