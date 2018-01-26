/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.mhpoptimization;

import java.util.Collection;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGForward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.SimpleMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

/**
 * Utility class which provides the possiblity to prune interference edges from
 * a given SDGs using MHP information, for example the results of a preliminary
 * MHP analysis.
 * 
 * @author Martin Mohr
 */
public final class PruneInterferences {

	private PruneInterferences() {}

	private static final Logger debug = Log.getLogger(Log.L_SDG_INTERFERENCE_DEBUG);
	private static final boolean IS_DEBUG = debug.isEnabled();
	
	/**
	 * Convenience method for {@link #prunePreprocessedCSDG(SDG, MHPType)}. It ensures that the SDG has been preprocessed, so that
	 * the required thread information is available. 
	 * @param g SDG to prune interference edges from
	 * @param mhpType determines which MHP algorithm is used (see {@link #prunePreprocessedCSDG(SDG, MHPType)}
	 */
	public static final void preprocessAndPruneCSDG(SDG g, MHPAnalysis mhpAnalysis) {
		CSDGPreprocessor.preprocessSDG(g);
		pruneInterferences(g, mhpAnalysis);
	}
	
	
	/**
	 * Prunes spurious interference edges from the given SDG based on the given MHP analysis result.<br>
	 * The given SDG is assumed to be preprocessed.<br>
	 * Note, that the SDG is altered by this method!
	 * @param graph SDG to prune interference edges from
	 * @param mhp mhp analysis result of given SDG which is to be taken as a basis for the pruning
	 */
	public static final void pruneInterferences(SDG graph, MHPAnalysis mhp) {
		if (mhp == null) return;
		LinkedList<SDGEdge> remove = new LinkedList<SDGEdge>();
		int all = 0;
		int x = 0;
		
		CFGForward forw = new CFGForward(ICFGBuilder.extractICFGIncludingJoins(graph));
		for (SDGEdge e : graph.edgeSet()) {
			if (e.getKind() == SDGEdge.Kind.INTERFERENCE
					|| e.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {
				all++;
				
				/**
				 * If we prune an interference edge solely because the connected nodes cannot happen in parallel, this can lead to unsound results.
				 * Thus, we have to be a bit more careful here:
				 * (i) Write-Write interferences can safely be ignored because the order of the writing statements is fixed (albeit we do not know it).
				 * (ii) If the edge in question is an interference edge, which connects a reading statement r with a writing statement w (of the same shared variable),
				 * then we prune the interference edge if either
				 * (a) the two statements are guaranteed to be in the same thread or
				 * (b) w can be reached from r in the control-flow-graph (including joins).
				 * If neither of these statements is true for the given interference edge, then we leave the edge where it is.
				 * Not that this is a bit of a hack because we use interference edges to model information flow which should be modeled by side-effects propagated to join sites.
				 * But until this interference pruning analysis is integrated into the SDG building process itself (where we have the chance to do it right), we stick
				 * to this hack-fix to avoid a soundness leak here.
				 */
				if (!mhp.isParallel(e.getSource(), e.getTarget()) && (guaranteedSameThread(e, graph.getThreadsInfo()) || !(e.getKind() == SDGEdge.Kind.INTERFERENCE && interThreadFlowPossible(e, forw)))) {
					remove.add(e);
					x++;
				}
			}
		}
	
		for (SDGEdge e : remove) {
			if (IS_DEBUG) debug.outln("Edge between " + e.getSource() + " and " + e.getTarget() + " of kind " + e.getKind() + " is spurious.");
			graph.removeEdge(e);
		}
	
		if (IS_DEBUG) debug.outln("	" + x + " of " + all + " edges removed");
		
		return;
	}
	
	/**
	 * The source and target nodes are guaranteed to be in the same thread, if 
	 * <ol>
	 * <li>they both have been assigned the same thread ID and no other thread ID's</li>
	 * <li>the thread with this ID is not dynamic</li>
	 * @param e edge to check
	 * @param ti provides the dynamicity information
	 * @return whether the two end-nodes of the given edge are guaranteed to be in the same thread
	 */
	private static boolean guaranteedSameThread(SDGEdge e, ThreadsInformation ti) {
		int[] threadsSrc = e.getSource().getThreadNumbers();
		int[] threadsTgt = e.getTarget().getThreadNumbers();
		return threadsSrc.length == 1 && threadsTgt.length == 1 && threadsSrc[0] == threadsTgt[0]
					&& !ti.getThread(threadsSrc[0]).isDynamic();
	}
	
	/**
	 * Uses the given forward slicer to determine whether the source of the given edge can reach the target of the given edge in the control-flow graph (including joins).
	 * The slicer is given as parameter here for efficiency reasons. It was initialized with the control-flow graph to be used for slicing.
	 * @param e edge to check
	 * @param forw forward slicer to use for reachability analysis
	 * @return whether the source of the given edge can reach the target of the given edge in the control-flow graph (including joins)
	 */
	private static boolean interThreadFlowPossible(SDGEdge e, CFGForward forw) {
		return forw.slice(e.getSource()).contains(e.getTarget());
	}

}
