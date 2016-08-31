/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.

 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.interference;



import edu.kit.joana.wala.core.AllocationSiteFinder;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.SDGBuilder;

/**
 * Finds the possible allocation sites for calls of Thread.start() or methods which override 
 * Thread.run() or Thread.join().
 * @author Martin Mohr
 */
public final class ThreadAllocationSiteFinder extends AllocationSiteFinder {

	public ThreadAllocationSiteFinder(final SDGBuilder sdg) {
		super(sdg);
	}

	protected boolean isInterestingCall(PDG callingCtx, PDGNode call) {
		if (callOfThreadStart(callingCtx, call) || callOfThreadJoin(callingCtx, call) || callOfThreadRunOverriding(callingCtx, call)) {
			debug.outln("Call node with id " + call.getId() + " is interesting.");
			return true;
		} else {
			return false;
		}
	}

	boolean callOfThreadStart(PDG callingCtx, PDGNode call) {
		return ThreadStartLocator.isCallToTarget(builder, callingCtx, call, ThreadInformationProvider.JavaLangThreadStart);
	}

	boolean callOfThreadJoin(PDG callingCtx, PDGNode call) {
		return ThreadStartLocator.isCallToTarget(builder, callingCtx, call, ThreadInformationProvider.JavaLangThreadJoin);
	}

	public boolean callOfThreadRunOverriding(PDG callingCtx, PDGNode call) {
		for (PDG possCallee: builder.getPossibleTargets(call)) {
			if (ThreadInformationProvider.overwritesThreadRun(builder.getNonPrunedWalaCallGraph().getClassHierarchy(), possCallee.cgNode.getMethod())) {
				return true;
			}
		}

		return false;
	}

}