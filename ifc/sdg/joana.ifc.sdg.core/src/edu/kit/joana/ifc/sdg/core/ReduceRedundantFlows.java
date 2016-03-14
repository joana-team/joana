/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.ViolationFilter;
import edu.kit.joana.ifc.sdg.core.violations.ViolationPartialMapper;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.barrier.BarrierChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.barrier.NonSameLevelBarrierChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.barrier.SimpleThreadBarrierChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.CSBarrierSlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.conc.I2PBarrierBackward;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.util.Maybe;

/**
 * This class extends a given IFC checker with the ability to filter out 'redundant' flows. A flow
 * is 'redundant', if it is already covered by other flows. <br>
 * To identify a flow as redundant, barrier slicing is applied.
 * @author Martin Mohr
 */
public class ReduceRedundantFlows extends IFC<String> {
	
	private final IFC<String> baseIFC;
	private final BarrierChopper bs;
	
	private ReduceRedundantFlows(IFC<String> baseIFC, BarrierChopper bs) {
		super(baseIFC.getSDG(), baseIFC.getLattice());
		this.baseIFC = baseIFC;
		this.bs = bs;
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.IFC#checkIFlow()
	 */
	@Override
	public Collection<? extends IViolation<SecurityNode>> checkIFlow() throws NotInLatticeException {
		Collection<? extends IViolation<SecurityNode>> baseFlows = baseIFC.checkIFlow();
		Collection<SDGNode> sources = new SourceExtractor().map(baseFlows);
		Collection<SDGNode> sinks = new SinkExtractor().map(baseFlows);
		return new RedundantFilter(sources, sinks).filter(baseFlows);
	}
	
	private boolean isRedundant(IIllegalFlow<SecurityNode> v, Collection<SDGNode> sources, Collection<SDGNode> sinks) {
		bs.setBarrier(Collections.<SDGNode>emptySet());
		boolean flowWOBarrier = !bs.chop(v.getSource(), v.getSink()).isEmpty();
		
		if (!flowWOBarrier) {
			return false;
		}
		
		Set<SDGNode> barrier = new HashSet<SDGNode>();
		barrier.addAll(sources);
		barrier.addAll(sinks);
		bs.setBarrier(barrier);
		boolean flowWithBarrier = !bs.chop(v.getSource(), v.getSink()).isEmpty();
		
		/**
		 * if v.getSink() cannot be reached from v.getSource() without trespassing one of the other
		 * source/sink nodes, then violation v is redundant.
		 */
		return !flowWithBarrier;
	}
	
	/**
	 * Factory method for a redundant-flow-reducing ifc checker for sequential programs. Uses {@link CSBarrierSlicerBackward}
	 * as barrier slicer.
	 * @param baseIFC base ifc algorithm to decorate
	 * @return a redundant-flow-reducing ifc checker for sequential programs, which uses {@link CSBarrierSlicerBackward}
	 * as barrier slicer.
	 */
	public static final IFC<String> makeReducingSequentialIFC(IFC<String> baseIFC) {
		return new ReduceRedundantFlows(baseIFC, new NonSameLevelBarrierChopper(baseIFC.getSDG()));
	}
	
	/**
	 * Factory method for a redundant-flow-reducing ifc checker for concurrent programs. Uses {@link I2PBarrierBackward}
	 * as barrier slicer.
	 * @param baseIFC base ifc algorithm to decorate
	 * @return a redundant-flow-reducing ifc checker for concurrent programs, which uses {@link I2PBarrierBackward}
	 * as barrier slicer.
	 */
	public static final IFC<String> makeReducingConcurrentIFC(IFC<String> baseIFC) {
		return new ReduceRedundantFlows(baseIFC, new SimpleThreadBarrierChopper(baseIFC.getSDG()));
	}
	
	private class RedundantFilter extends ViolationFilter<SecurityNode> {


		private Collection<SDGNode> sources;
		private Collection<SDGNode> sinks;


		public RedundantFilter(Collection<SDGNode> sources, Collection<SDGNode> sinks) {
			this.sources = sources;
			this.sinks = sinks;
		}


		
		/* (non-Javadoc)
		 * @see edu.kit.joana.ifc.sdg.core.conc.ViolationFilter#acceptIllegalFlow(edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow)
		 */
		@Override
		protected boolean acceptIllegalFlow(IIllegalFlow<SecurityNode> iFlow) {
			return !isRedundant(iFlow, sources, sinks);
		}
		
	}
	

	private static class SourceExtractor extends ViolationPartialMapper<SecurityNode,SDGNode> {

		/* (non-Javadoc)
		 * @see edu.kit.joana.ifc.sdg.core.conc.ViolationMapper#mapIllegalFlow(edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow)
		 */
		@Override
		protected Maybe<SDGNode> maybeMapIllegalFlow(IIllegalFlow<SecurityNode> iFlow) {
			return Maybe.just((SDGNode) iFlow.getSource());
		}
	}
	
	private static class SinkExtractor extends ViolationPartialMapper<SecurityNode, SDGNode> {

		/* (non-Javadoc)
		 * @see edu.kit.joana.ifc.sdg.core.conc.ViolationMapper#mapIllegalFlow(edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow)
		 */
		@Override
		protected Maybe<SDGNode> maybeMapIllegalFlow(IIllegalFlow<SecurityNode> iFlow) {
			return Maybe.just((SDGNode) iFlow.getSink());
		}
	}
	

}
