/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.exceptions.nullpointer;


import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.deprecated.jsdg.exceptions.ExceptionPrunedCFGAnalysis;

/**
 * Tries to detect impossible NullPointerExceptions and removes impossible
 * control flow from the CFG.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class NullPointerAnalysis {

    public static final TypeReference[] DEFAULT_IGNORE_EXCEPTIONS = {
    	TypeReference.JavaLangOutOfMemoryError,
    	TypeReference.JavaLangExceptionInInitializerError,
    	TypeReference.JavaLangNegativeArraySizeException
    };

	public static ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock> createIntraproceduralExplodedCFGAnalysis() {
		return createIntraproceduralExplodedCFGAnalysis(DEFAULT_IGNORE_EXCEPTIONS);
	}

	public static ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock>
	createIntraproceduralExplodedCFGAnalysis(TypeReference[] ignoredExceptions) {
		return new ExplodedCFGNullPointerAnalysis(ignoredExceptions);
	}

	public static ExceptionPrunedCFGAnalysis<SSAInstruction, ISSABasicBlock> createIntraproceduralSSACFGAnalyis() {
		return createIntraproceduralSSACFGAnalyis(DEFAULT_IGNORE_EXCEPTIONS);
	}

	public static ExceptionPrunedCFGAnalysis<SSAInstruction, ISSABasicBlock>
	createIntraproceduralSSACFGAnalyis(TypeReference[] ignoredExceptions) {
		return new SSACFGNullPointerAnalysis(ignoredExceptions);
	}

	private static class ExplodedCFGNullPointerAnalysis extends ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock> {

		private final TypeReference[] ignoredExceptions;

		private ExplodedCFGNullPointerAnalysis(TypeReference[] ignoredExceptions) {
			// intraprocedural analysis does not need callgraph and points-to analysis
			super(null, null, null);
			this.ignoredExceptions = (ignoredExceptions != null ? ignoredExceptions.clone() : DEFAULT_IGNORE_EXCEPTIONS);
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.exceptions.ExceptionPrunedCFGAnalysis#getOriginal(com.ibm.wala.ipa.callgraph.CGNode)
		 */
		@Override
		public ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> getOriginal(CGNode method) throws UnsoundGraphException {
			return ExplodedControlFlowGraph.make(method.getIR());
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.exceptions.ExceptionPrunedCFGAnalysis#getPruned(com.ibm.wala.ipa.callgraph.CGNode)
		 */
		@Override
		public ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> getPruned(CGNode method, IProgressMonitor progress)
		throws UnsoundGraphException, CancelException {

			ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> orig = getOriginal(method);
			IntraprocNullPointerAnalysis<IExplodedBasicBlock> intra =
				new IntraprocNullPointerAnalysis<IExplodedBasicBlock>(method, orig, ignoredExceptions);

			intra.run(progress);

			return intra.getPrunedCfg();
		}
	}

	private static class SSACFGNullPointerAnalysis extends ExceptionPrunedCFGAnalysis<SSAInstruction, ISSABasicBlock> {

		private final TypeReference[] ignoredExceptions;

		private SSACFGNullPointerAnalysis(TypeReference[] ignoredExceptions) {
			// intraprocedural analysis does not need callgraph and points-to analysis
			super(null, null, null);
			this.ignoredExceptions = (ignoredExceptions != null ? ignoredExceptions.clone() : null);
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.exceptions.ExceptionPrunedCFGAnalysis#getOriginal(com.ibm.wala.ipa.callgraph.CGNode)
		 */
		@Override
		public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getOriginal(
				CGNode method) throws UnsoundGraphException {
			return method.getIR().getControlFlowGraph();
		}

		/* (non-Javadoc)
		 * @see edu.kit.joana.deprecated.jsdg.exceptions.ExceptionPrunedCFGAnalysis#getPruned(com.ibm.wala.ipa.callgraph.CGNode, org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getPruned(
				CGNode method, IProgressMonitor progress)
				throws UnsoundGraphException, CancelException {
			ControlFlowGraph<SSAInstruction, ISSABasicBlock> orig = getOriginal(method);
			IntraprocNullPointerAnalysis<ISSABasicBlock> intra =
				new IntraprocNullPointerAnalysis<ISSABasicBlock>(method, orig, ignoredExceptions);

			intra.run(progress);

			return intra.getPrunedCfg();
		}

	}

}
