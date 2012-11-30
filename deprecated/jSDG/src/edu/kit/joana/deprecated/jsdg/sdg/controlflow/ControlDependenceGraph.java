/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.controlflow;

import java.util.Collection;
import java.util.Iterator;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.cfg.Util;
import com.ibm.wala.util.graph.Acyclic;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.dominators.DominanceFrontiers;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.labeled.SlowSparseNumberedLabeledGraph;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntPair;

import edu.kit.joana.deprecated.jsdg.util.Log;

/**
 * Finally we build our own control dependence graph, as WALAs version is buggy
 * non-standard and hard to debug.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ControlDependenceGraph<T> extends SlowSparseNumberedLabeledGraph<T, ControlDependenceGraph.Edge> {

	private final T entry;
	private final T exit;

	public static enum Edge { CD, CD_TRUE, CD_FALSE, CD_EX, CD_NO_EX, UN};

	private ControlDependenceGraph(final T entry, final T exit) {
		super(Edge.CD); /* default is control dependence */
		this.entry = entry;
		this.exit = exit;
	}

	public static <T> ControlDependenceGraph<T> build(final Graph<T> cfg, final T entry, final T exit) {
		if (entry == null || exit == null) {
			throw new IllegalArgumentException("Entry and exit nodes may not be null.");
		}

		if (!cfg.containsNode(entry) || !cfg.containsNode(exit)) {
			throw new IllegalArgumentException("Cfg does not contain entry or exit node.");
		}

		if (!cfg.hasEdge(entry, exit)) {
			throw new IllegalArgumentException("We do stuff by the book: Our cfgs always have an edge from entry to exit!");
		}

		ControlDependenceGraph<T> cdg = new ControlDependenceGraph<T>(entry, exit);
		cdg.computeControlDependence(cfg);
		cdg.removeBackEdges();

		return cdg;
	}

	@SuppressWarnings("unchecked")
	private final <I> void computeControlDependence(final Graph<T> cfg) {
		for (T node : cfg) {
			addNode(node);
		}

		ControlFlowGraph<I, IBasicBlock<I>> walaCfg = null;

		if (cfg instanceof ControlFlowGraph<?, ?>) {
			walaCfg = (ControlFlowGraph<I, IBasicBlock<I>>) cfg;
		}

		DominanceFrontiers<T> domFront = new DominanceFrontiers<T>(GraphInverter.invert(cfg), exit);

	    for (T node : cfg) {
	        boolean noDom = true;

	        for (Iterator<T> ns2 = domFront.getDominanceFrontier(node); ns2.hasNext();) {
	          T frontier = ns2.next();

	          noDom = false;

	          if (walaCfg != null) {
	        	  IBasicBlock<I> front = (IBasicBlock<I>) frontier;
	        	  if (front.getLastInstructionIndex() >= 0 && Util.endsWithConditionalBranch(walaCfg, front)) {
	        		  IBasicBlock<?> trueSucc = Util.getTakenSuccessor(walaCfg, front);
	        		  boolean isTrue = (node == trueSucc) || domFront.isDominatedBy(node, (T) trueSucc);
	        		  if (isTrue) {
	        			  addEdge(frontier, node, Edge.CD_TRUE);
	        		  } else {
	        			  addEdge(frontier, node, Edge.CD_FALSE);
	        		  }
	        	  } else {
	        		  Collection<IBasicBlock<I>> normSuccs = walaCfg.getNormalSuccessors(front);
	        		  if (normSuccs != null && !normSuccs.isEmpty()) {
	        			  boolean isExcSucc = true;

	        			  for (IBasicBlock<?> succ : normSuccs) {
	        				  if (succ == node || domFront.isDominatedBy(node, (T) succ)) {
	        					  isExcSucc = false;
	        					  break;
	        				  }
	        			  }

	        			  if (isExcSucc) {
	        				  addEdge(frontier, node, Edge.CD_EX);
	        			  } else {
	        				  addEdge(frontier, node, Edge.CD_NO_EX);
	        			  }
	        		  } else {
	        			  addEdge(frontier, node, Edge.UN);
	        		  }
	        	  }
	          }

	          // add default edges
	          addEdge(frontier, node);
	        }

	        if (noDom && node != entry) {
	        	if (walaCfg != null) {
      			  addEdge(entry, node, Edge.UN);
	        	}

	        	addEdge(entry, node);
	        }
	    }
	}

	private final void removeBackEdges() {
		IBinaryNaturalRelation backEdges = Acyclic.computeBackEdges(this, entry);

		boolean hasBackEdge = false;

		for (IntPair be : backEdges) {
			hasBackEdge = true;

			T from = getNode(be.getX());
			T to = getNode(be.getY());

			for (Edge label : getEdgeLabels(from, to)) {
				removeEdge(from, to, label);
			}
		}

		if (hasBackEdge) {
			Log.info("Found backedge in control dependencies.");
		}
	}

}
