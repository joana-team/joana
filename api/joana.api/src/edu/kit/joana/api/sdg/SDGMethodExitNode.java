/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;


import edu.kit.joana.ifc.sdg.util.JavaType;

public class SDGMethodExitNode implements SDGProgramPart {

	private SDGMethod owningMethod;
	private JavaType type;

	SDGMethodExitNode(SDGMethod owningMethod, JavaType type) {
		this.owningMethod = owningMethod;
		this.type = type;
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

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#getOwningMethod()
	 */
	@Override
	public SDGMethod getOwningMethod() {
		return owningMethod;
	}
}
