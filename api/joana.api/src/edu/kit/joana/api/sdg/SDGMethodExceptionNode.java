/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

/**
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public class SDGMethodExceptionNode implements SDGProgramPart {
	private SDGMethod owningMethod;

	SDGMethodExceptionNode(SDGMethod owningMethod) {
		this.owningMethod = owningMethod;
	}

	@Override
	public String toString() {
		return "exception-out of method " + getOwningMethod().getSignature().toHRString();
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#acceptVisitor(edu.kit.joana.api.sdg.SDGProgramPartVisitor, java.lang.Object)
	 */
	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitException(this, data);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#getOwningMethod()
	 */
	@Override
	public SDGMethod getOwningMethod() {
		return owningMethod;
	}
}
