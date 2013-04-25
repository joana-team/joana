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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.core.violations.Violation;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.barrier.BarrierChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.barrier.NonSameLevelBarrierChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.barrier.SimpleThreadBarrierChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.CSBarrierSlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.barrier.conc.I2PBarrierBackward;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;

/**
 * This class extends a given IFC checker with the ability to filter out 'redundant' flows. A flow
 * is 'redundant', if it is already covered by other flows. <br>
 * To identify a flow as redundant, barrier slicing is applied.
 * @author Martin Mohr
 */
public class ReduceRedundantFlows extends IFC {
	
	private final IFC baseIFC;
	private final BarrierChopper bs;
	
	private ReduceRedundantFlows(IFC baseIFC, BarrierChopper bs) {
		super(baseIFC.getSDG(), baseIFC.getLattice());
		this.baseIFC = baseIFC;
		this.bs = bs;
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.IFC#checkIFlow()
	 */
	@Override
	public Collection<Violation> checkIFlow() throws NotInLatticeException {
		Collection<Violation> baseFlows = baseIFC.checkIFlow();
		Collection<SDGNode> sources = computeSources(baseFlows);
		Collection<SDGNode> sinks = computeSinks(baseFlows);
		List<Violation> reducedFlows = new LinkedList<Violation>();
		for (Violation v : baseFlows) {
			if (!isRedundant(v, sources, sinks)) {
				reducedFlows.add(v);
			}
		}
		return reducedFlows;
	}
	
	private static Collection<SDGNode> computeSources(final Collection<Violation> flows) {
		final Collection<SDGNode> ret = new LinkedList<SDGNode>();
		for (Violation v : flows) {
			ret.add(v.getSource());
		}
		return ret;
	}
	
	private static Collection<SDGNode> computeSinks(final Collection<Violation> flows) {
		final Collection<SDGNode> ret = new LinkedList<SDGNode>();
		for (Violation v : flows) {
			ret.add(v.getSink());
		}
		return ret;
	}
	
	private boolean isRedundant(Violation v, Collection<SDGNode> sources, Collection<SDGNode> sinks) {
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
	public static final IFC makeReducingSequentialIFC(IFC baseIFC) {
		return new ReduceRedundantFlows(baseIFC, new NonSameLevelBarrierChopper(baseIFC.getSDG()));
	}
	
	/**
	 * Factory method for a redundant-flow-reducing ifc checker for concurrent programs. Uses {@link I2PBarrierBackward}
	 * as barrier slicer.
	 * @param baseIFC base ifc algorithm to decorate
	 * @return a redundant-flow-reducing ifc checker for concurrent programs, which uses {@link I2PBarrierBackward}
	 * as barrier slicer.
	 */
	public static final IFC makeReducingConcurrentIFC(IFC baseIFC) {
		return new ReduceRedundantFlows(baseIFC, new SimpleThreadBarrierChopper(baseIFC.getSDG()));
	}
	

}
