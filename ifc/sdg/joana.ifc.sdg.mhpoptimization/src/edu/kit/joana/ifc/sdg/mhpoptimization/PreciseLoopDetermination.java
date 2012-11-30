/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.mhpoptimization;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgrapht.alg.StrongConnectivityInspector;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;

/**
 * This is an analysis for finding all nodes in a control-flow graph, which may
 * be contained in a loop and could thus be executed indefinitely often. First,
 * all intra-procedural strongly-connected components (SCCs) are computed. Then,
 * a node may be contained in a loop, iff:
 * <ol>
 * <li>it is contained in one of these intra-procedural SCCs, or</li>
 * <li>it is contained in a procedure called by a node, which may be contained
 * in a loop.</li>
 * </ol>
 * Note that the given control-flow graph is not copied and changed temporarily by this class,
 * but all changes are reversed, so in effect there is no change. This leads to problems,
 * if the given {@link CFG} implementation forbids changes.
 * Plus, it is assumed, that the given control-flow graph is not changed after 
 * the analysis is finished. This means, that results could be unsound or imprecise, 
 * if the given control-flow graph is changed between two calls {@link #isInALoop(SDGNode)}.
 * 
 * @author Martin Mohr
 */
public class PreciseLoopDetermination implements LoopDetermination {

	/** the control-flow graph to analyze */
	private final CFG cfg;

	/** nodes which could be contained in a loop */
	private final Set<SDGNode> possibleLoopNodes = new HashSet<SDGNode>();

	/** Records if the computation is finished. */
	private boolean isFinished = false;

	/**
	 * Constructs a new LoopDetermination object. 
	 * @param cfg the control-flow graph to analyze
	 */
	public PreciseLoopDetermination(CFG cfg) {
		this.cfg = cfg;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.mhpoptimization.LoopDetermination#isInALoop(edu.kit.joana.ifc.sdg.graph.slicer.graph.Context)
	 */
	@Override
	public boolean isInALoop(Context c) {
		for (SDGNode n : c.getCallStack()) {
			if (isInALoop(n)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns whether the given sdg node may be contained in a loop.
	 * @param node sdg node to check
	 * @return {@code true} if the given sdg node may be contained in a loop, {@code false} otherwise
	 */
	private boolean isInALoop(SDGNode node) {
		if (!isFinished()) {
			compute();
		}

		return possibleLoopNodes.contains(node);
	}

	private boolean isFinished() {
		return isFinished;
	}

	private void compute() {
		List<SDGEdge> removedEdges = removeCallAndReturnEdges();
		List<Set<SDGNode>> sccs = computeSCCs();
		addEdges(removedEdges);
		saturate(sccs);
		finished();
	}

	/**
	 * Removes all the call and return edges from the given control-flow graph
	 * @return
	 */
	private List<SDGEdge> removeCallAndReturnEdges() {
		List<SDGEdge> toRemove = new LinkedList<SDGEdge>();
		for (SDGEdge e : cfg.edgeSet()) {
			if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.RETURN) {
				toRemove.add(e);
			}
		}
		
		cfg.removeAllEdges(toRemove);
		
		return toRemove;
	}
	
	/**
	 * Computes the strongly-connected components of the {@link control-flow graph}.
	 * @return list of all strongly-connected components of the control-flow graph
	 */
	private List<Set<SDGNode>> computeSCCs() {
		StrongConnectivityInspector<SDGNode, SDGEdge> sci = new StrongConnectivityInspector<SDGNode, SDGEdge>(cfg);
		return sci.stronglyConnectedSets();
	}
	
	/**
	 * Saturates the set of possible loop nodes using the following two rules:
	 * <ol>
	 * <li>all nodes contained in a SCC containing at least two nodes are possible loop nodes.</li>
	 * <li>If a possible loop node is a call, then all nodes contained in the called procedure
	 * are also possible loop nodes.</li>
	 * </ol>
	 * @param sccs SCCs to begin saturation with
	 */
	private void saturate(List<Set<SDGNode>> sccs) {
		
		LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
		
		/**
		 * first step: initialize
		 */
		for (Set<SDGNode> scc : sccs) {
			if (scc.size() >= 2) {
				worklist.addAll(scc);
			}
		}
		
		/**
		 * second step: saturate
		 */
		while (!worklist.isEmpty()) {
			SDGNode next = worklist.poll();
			possibleLoopNodes.add(next);
			
			/**
			 * if the currently examined node is a call node, then all nodes contained in all
			 * possible call targets are possible loop nodes
			 */
			if (next.getKind() == SDGNode.Kind.CALL) {
				for (SDGEdge e: cfg.outgoingEdgesOf(next)) {
					if (e.getKind() == SDGEdge.Kind.CALL) {
						SDGNode eTarget = e.getTarget();
						for (SDGNode n : cfg.getNodesOfProcedure(eTarget)) {
							if (!possibleLoopNodes.contains(n)) {
								worklist.add(n);
							}
						}
					}
				}
			}
			
		}
		
		
	}
	
	/**
	 * Adds the given edges to the {@link #cfg control-flow graph}.
	 * @param removedEdges edges to add.
	 */
	private void addEdges(List<SDGEdge> removedEdges) {
		for (SDGEdge e : removedEdges) {
			cfg.addEdge(e.getSource(), e.getTarget(), e);
		}
	}


	private void finished() {
		isFinished = true;
	}
	
	

}
