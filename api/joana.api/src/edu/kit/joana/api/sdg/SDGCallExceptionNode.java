/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * @author Martin Mohr
 */
public class SDGCallExceptionNode extends SDGNodeWrapper implements SDGCallPart {
	
	private SDGCall owningCall;
	
	/**
	 * @param node
	 * @param owningMethod
	 */
	public SDGCallExceptionNode(SDGNode node, SDGCall owningCall) {
		super(node, owningCall.getOwningMethod());
		this.owningCall = owningCall;
	}
	
	@Override
	public SDGCall getOwningCall() {
		return owningCall;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#acceptVisitor(edu.kit.joana.api.sdg.SDGProgramPartVisitor, java.lang.Object)
	 */
	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitCallExceptionNode(this, data);
	}
	
	@Override
	public String toString() {
		return owningCall.toString() + "->exc";
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGCallPart#acceptVisitor(edu.kit.joana.api.sdg.SDGCallPartVisitor)
	 */
	@Override
	public void acceptVisitor(SDGCallPartVisitor v) {
		v.visitExceptionNode(this);
	}

}
