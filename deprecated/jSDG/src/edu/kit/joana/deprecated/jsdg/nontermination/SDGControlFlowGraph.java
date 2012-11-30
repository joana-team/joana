/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.nontermination;

import java.util.Iterator;
import java.util.Set;


import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.EntryNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.NormalNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.EdgeType;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParameter;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class SDGControlFlowGraph extends SlowSparseNumberedGraph<AbstractPDGNode> {

	private SDGControlFlowGraph() {}

	public static NumberedGraph<AbstractPDGNode> create(PDG pdg, boolean includeParams) {
		SDGControlFlowGraph cfg = new SDGControlFlowGraph();

		for (AbstractPDGNode node : pdg) {
			if (node.getPdgId() != pdg.getId()) {
				continue;
			}

			cfg.addNode(node);

			pdg.getSuccNodeNumbers(node, EdgeType.CF);
		}

		for (AbstractPDGNode node : cfg) {
			Iterator<? extends AbstractPDGNode> it = pdg.getSuccNodes(node, EdgeType.CF);
			while (it.hasNext()) {
				AbstractPDGNode to = it.next();
				cfg.addEdge(node, to);
			}
		}

		if (!includeParams) {
			removeParameterNodes(cfg);
		}

		assert assertControlFlow(cfg);

		return cfg;
	}

	private static boolean assertControlFlow(SDGControlFlowGraph cfg) {
		for (AbstractPDGNode node : cfg) {
			final int succ = cfg.getSuccNodeCount(node);
			final int pred = cfg.getPredNodeCount(node);
			if (succ == 0 && pred == 0) {
				System.err.println("No control flow for " + node);
			} else if (succ == 0 && !(node.isParameterNode() && ((IParameter) node).isExit())) {
				System.err.println("No outgoing control flow for " + node);
			} else if (pred == 0 && !(node instanceof EntryNode)) {
				System.err.println("No incoming control flow for " + node);
			}
		}

		return true;
	}

	private static void removeParameterNodes(SDGControlFlowGraph cfg) {
		Set<AbstractPDGNode> params = HashSetFactory.make();
		for (AbstractPDGNode node : cfg) {
			if (isParam(node)) {
				params.add(node);
			}
		}

		for (AbstractPDGNode param : params) {
			removeNode(cfg, param);
		}
	}

	private static void removeNode(SDGControlFlowGraph cfg, AbstractPDGNode node) {
		Iterator<? extends AbstractPDGNode> itPred = cfg.getPredNodes(node);
		while (itPred.hasNext()) {
			AbstractPDGNode pred = itPred.next();
			Iterator<? extends AbstractPDGNode> itSucc = cfg.getSuccNodes(node);
			while (itSucc.hasNext()) {
				AbstractPDGNode succ = itSucc.next();
				cfg.addEdge(pred, succ);
			}
		}

		cfg.removeNodeAndEdges(node);
	}

	private static boolean isParam(AbstractPDGNode node) {
		return node.isParameterNode() && (!((IParameter) node).isExit() || ((IParameter) node).isException());
	}

	private static final int ARTIFICIAL_ID = -1;

	public static final AbstractPDGNode createArtificialNode() {
		AbstractPDGNode node = new NormalNode(ARTIFICIAL_ID);
		node.setLabel("artificial");
		return node;
	}

	public static final boolean isArtificial(AbstractPDGNode node) {
		return node.getPdgId() == ARTIFICIAL_ID;
	}

}
