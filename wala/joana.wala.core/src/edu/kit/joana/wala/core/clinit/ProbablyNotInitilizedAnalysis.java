/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.clinit;

import java.util.Collection;

import com.ibm.wala.dataflow.IFDS.ICFGSupergraph;
import com.ibm.wala.dataflow.IFDS.IMergeFunction;
import com.ibm.wala.dataflow.IFDS.IPartiallyBalancedFlowFunctions;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.PartiallyBalancedTabulationProblem;
import com.ibm.wala.dataflow.IFDS.PathEdge;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.MutableMapping;

/**
 * Computes interprocedural access to classes that may not have been initialized and therefore a static initializer
 * may be called.
 * 
 * This class is a stub, perhaps someday someone will implement it.
 * 
 * Access to classes is issued by:
 *  - a static field access
 *  - a call to a static method
 *  - a new instance operation
 */
public class ProbablyNotInitilizedAnalysis {

	@SuppressWarnings("unused")
	private final IClassHierarchy cha;
	private final ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> sg;
	private final NotInitializedDomain dom = new NotInitializedDomain();

	public ProbablyNotInitilizedAnalysis(final CallGraph cg, final AnalysisCache cache) {
		this.cha = cg.getClassHierarchy();
		this.sg = ICFGSupergraph.make(cg, cache);
	}

	public TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<CGNode, Integer>> analyze() {
		return null;
	}

	public ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> getSupergraph() {
		return sg;
	}

	public TabulationDomain<Pair<CGNode, Integer>, BasicBlockInContext<IExplodedBasicBlock>> getDomain() {
		return dom;
	}

	/**
	 * controls numbering of class access instructions for use in tabulation
	 */
	private class NotInitializedDomain extends MutableMapping<Pair<CGNode, Integer>> implements
			TabulationDomain<Pair<CGNode, Integer>, BasicBlockInContext<IExplodedBasicBlock>> {

		public boolean hasPriorityOver(PathEdge<BasicBlockInContext<IExplodedBasicBlock>> p1,
				PathEdge<BasicBlockInContext<IExplodedBasicBlock>> p2) {
			// don't worry about worklist priorities
			return false;
		}
	}

	@SuppressWarnings("unused")
	private class NotInitializedProblem implements
			PartiallyBalancedTabulationProblem<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<CGNode, Integer>> {

	    private final Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> initialSeeds = collectInitialSeeds();

		/**
		 * we use the entry block of the CGNode as the fake entry when
		 * propagating from callee to caller with unbalanced parents
		 */
		public BasicBlockInContext<IExplodedBasicBlock> getFakeEntry(BasicBlockInContext<IExplodedBasicBlock> node) {
			final CGNode cgNode = node.getNode();
			return getFakeEntry(cgNode);
		}

		/**
		 * we use the entry block of the CGNode as the "fake" entry when
		 * propagating from callee to caller with unbalanced parents
		 */
		private BasicBlockInContext<IExplodedBasicBlock> getFakeEntry(final CGNode cgNode) {
			final BasicBlockInContext<IExplodedBasicBlock>[] entriesForProcedure = sg.getEntriesForProcedure(cgNode);
			assert entriesForProcedure.length == 1;

			return entriesForProcedure[0];
		}

		public TabulationDomain<Pair<CGNode, Integer>, BasicBlockInContext<IExplodedBasicBlock>> getDomain() {
			return dom;
		}

		/**
		 * we don't need a merge function; the default unioning of tabulation
		 * works fine
		 */
		public IMergeFunction getMergeFunction() {
			return null;
		}

		public ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> getSupergraph() {
			return sg;
		}

		private Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> collectInitialSeeds() {
			return null;
		}

		@Override
		public Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> initialSeeds() {
			return initialSeeds;
		}

		@Override
		public IPartiallyBalancedFlowFunctions<BasicBlockInContext<IExplodedBasicBlock>> getFunctionMap() {
			// TODO Auto-generated method stub
			return null;
		}

	}
}
