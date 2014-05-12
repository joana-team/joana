/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.

 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;


/**
 * Finds the possible allocation sites for the receiver object of every method call.
 * @author Martin Mohr
 */
public class AllCallsAllocationSiteFinder extends AllocationSiteFinder {

	public AllCallsAllocationSiteFinder(SDGBuilder builder) {
		super(builder);
	}

	@Override
	protected boolean isInterestingCall(PDG callingCtx, PDGNode call) {
		return true;
	}

}
