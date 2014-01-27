/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;


import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.ifc.sdg.util.JavaType.Format;

public class SDGMethodExitNode extends SDGNodeWrapper {

	private JavaType type;

	SDGMethodExitNode(SDGNode node, SDGMethod owningMethod) {
		super(node, owningMethod);
		this.type = JavaType.parseSingleTypeFromString(node.getType(), Format.BC);
	}

	@Override
	public SDGNode getNode() {
		return node;
	}

	public JavaType getType() {
		return type;
	}

	
	@Override
	public String toString() {
		return "exit of method " + getOwningMethod().getSignature().toHRString();
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#acceptVisitor(edu.kit.joana.api.sdg.SDGProgramPartVisitor, java.lang.Object)
	 */
	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitExit(this, data);
	}
}
