/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import edu.kit.joana.ifc.sdg.util.JavaType;

/**
 * @author Martin Mohr
 */
public class SDGActualParameter implements SDGCallPart  {
	private final SDGCall owningCall;
	private final int index;
	private final JavaType type;

	SDGActualParameter(SDGCall owningCall, int index, JavaType type) {
		this.owningCall = owningCall;
		this.index = index;
		this.type = type;
	}

	public int getIndex() {
		return index;
	}

	public JavaType getType() {
		return type;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#acceptVisitor(edu.kit.joana.api.sdg.SDGProgramPartVisitor, java.lang.Object)
	 */
	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitActualParameter(this, data);
	}

	@Override
	public SDGCall getOwningCall() {
		return owningCall;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#getOwningMethod()
	 */
	@Override
	public SDGMethod getOwningMethod() {
		return owningCall.getOwningMethod();
	}

	@Override
	public String toString() {
		return owningCall.toString() + "->act" + index;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGCallPart#acceptVisitor(edu.kit.joana.api.sdg.SDGCallPartVisitor)
	 */
	@Override
	public void acceptVisitor(SDGCallPartVisitor v) {
		v.visitActualParameter(this);
	}

}
