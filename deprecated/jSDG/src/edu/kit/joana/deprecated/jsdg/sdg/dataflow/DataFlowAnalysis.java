/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.dataflow;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.deprecated.jsdg.Messages;
import edu.kit.joana.deprecated.jsdg.sdg.IntermediatePDG;
import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.dataflow.CFGWithParameterNodes.CFGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CatchNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ConstantPhiValueNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.EntryNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ExpressionNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.HeapAccessCompound;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.IPDGNodeVisitor;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.NormalNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.PhiValueNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.PredicateNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.SyncNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IModRef;
import edu.kit.joana.deprecated.jsdg.util.Debug;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;

/**
 * Does the intraprocedural dataflow analysis for a pdg. This is called when
 * all objecttrees have been created and propagated.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class DataFlowAnalysis {

	private final IntermediatePDG pdg;
	private Collection<AbstractPDGNode> statements;
	private OrdinalSetMapping<AbstractPDGNode> mapping;
	private ITransferFunctionProvider<CFGNode, BitVectorVariable> transfer;
	private Map<AbstractPDGNode, OrdinalSet<AbstractPDGNode>> lastDefMap;

	private final CFGWithParameterNodes ecfg;
	private IModRef modRef;

	private DataFlowAnalysis(IntermediatePDG pdg) {
		this.pdg = pdg;
		this.lastDefMap = null;
		ExplodedControlFlowGraph explCfg = ExplodedControlFlowGraph.make(pdg.getIR());
		this.ecfg =	new CFGWithParameterNodes(explCfg, pdg);
	}

	/**
	 * Compute the heap based datadependencies for all pdgs
	 * @throws CancelException
	 */
	public static void computeDataDependence(Set<PDG> pdgs, IProgressMonitor progress) throws CancelException {
		progress.subTask(Messages.getString("SDG.SubTask_Data_Dep")); //$NON-NLS-1$

		for (PDG pdg : pdgs) {
			if (pdg.isStub() || (pdg.getIR() == null && pdg.getMethod().isNative())) {
				// No IR for native method -> we have to skip the heap data
				// dependencies
				continue;
			}

			computeDataDependence(pdg, progress);

			progress.worked(1);
		}

		progress.done();
	}

	/**
	 * Computes the heapdata dependencies of the specified pdg using the
	 * points-to information of wala and a killdall stlye last reached defs
	 * framework. Adds the calculated dependencies as normal data dependency
	 * (DD) edges to the pdg.
	 * @param pdg program dependency graph
	 * @throws CancelException
	 */
	private static void computeDataDependence(PDG pdg, IProgressMonitor progress) throws CancelException {
		Log.info("Computing heapdata dependencies for " + pdg);

		Map<AbstractPDGNode, OrdinalSet<AbstractPDGNode>> lastReachDefs =
			DataFlowAnalysis.computeLastReachDefs(pdg, progress);

		for (AbstractPDGNode node : lastReachDefs.keySet()) {
			OrdinalSet<AbstractPDGNode> lastDefs = lastReachDefs.get(node);
			// Hack for artificial sync fields. There is no initialization for these fields.
			assert (lastDefs != null) || (node instanceof SyncNode);

			if (lastDefs != null) {
				for (AbstractPDGNode lastDef : lastDefs) {
					pdg.addHeapDataDependency(lastDef, node);
				}
			}
		}
	}

	private static Map<AbstractPDGNode, OrdinalSet<AbstractPDGNode>>
	computeLastReachDefs(IntermediatePDG pdg, IProgressMonitor monitor) throws CancelException {
		DataFlowAnalysis dfa = new DataFlowAnalysis(pdg);

		return dfa.computeLastReachDefs(monitor);
	}

	/**
	 * Computes a map of last reaching definitions. Each node n of the pdg is
	 * mapped to a set of nodes that may define the value used at n.
	 *
	 * The sdg and cfg information is put into the BitVectorFramework of wala
	 * and the BitVectorSolver is used to do the computation. The tricky part
	 * is to put all data in the right way into the framework.
	 *
	 * @param monitor progress monitor that displays the progress of the analysis
	 * @return map of pdg node to set of nodes defining the value used at the node
	 * @throws CancelException
	 */
	private Map<AbstractPDGNode, OrdinalSet<AbstractPDGNode>> computeLastReachDefs(IProgressMonitor monitor)
	throws CancelException {
		if (lastDefMap == null) {
			prepareStatements(monitor);

			transfer = new ReachingDefsTransferFP(ecfg, mapping, modRef);

			BitVectorFramework<CFGNode, AbstractPDGNode> reachDef =
				new BitVectorFramework<CFGNode, AbstractPDGNode>(ecfg, transfer, mapping);

			BitVectorSolver<CFGNode> solver =
				new BitVectorSolver<CFGNode>(reachDef);

			solver.solve(monitor);

			lastDefMap = buildResult(solver);

			if (Debug.Var.PRINT_MOD_REF_KILL_GEN.isSet()) {
				Util.printModRefKillGen(mapping, modRef, ecfg, pdg, solver, transfer, lastDefMap);
			}
		}

		return lastDefMap;
	}

	private Map<AbstractPDGNode, OrdinalSet<AbstractPDGNode>>
	buildResult(BitVectorSolver<CFGNode> solver) {
		Map<AbstractPDGNode, OrdinalSet<AbstractPDGNode>> result =
			new HashMap<AbstractPDGNode, OrdinalSet<AbstractPDGNode>>();

		for (AbstractPDGNode node : mapping) {
			result.put(node, computeLastDefForNode(node, solver));
		}

		return result;
	}

	private class LastDefVisitor implements IPDGNodeVisitor {

		private OrdinalSet<AbstractPDGNode> result;
		private final BitVectorSolver<CFGNode> solver;

		private final OrdinalSet<AbstractPDGNode> emptySet =
			new OrdinalSet<AbstractPDGNode>(null, mapping);

		LastDefVisitor(final BitVectorSolver<CFGNode> solver) {
			this.solver = solver;
		}

		public OrdinalSet<AbstractPDGNode> getResult() {
			return result;
		}

		private OrdinalSet<AbstractPDGNode> intersect(BitVectorVariable refVar,
				BitVectorVariable inVar) {
			final IntSet ref = refVar.getValue();
			final IntSet in = inVar.getValue();
			if (in != null && ref != null) {
				final IntSet lastReach = ref.intersection(in);

				return new OrdinalSet<AbstractPDGNode>(lastReach, mapping);
			} else {
				return emptySet;
			}
		}

		public void visitCall(CallNode node) {
			result = emptySet;
		}

		public void visitEntry(EntryNode node) {
			result = emptySet;
		}

		public void visitExpression(ExpressionNode node) {
			result = emptySet;

//			SSAInstruction instr = pdg.getInstructionForNode(node);
//
//			assert (instr != null) : "An expression node should" +
//				" always have a corresponding ssa instruction: " + node;

			CFGNode cnode = ecfg.getNodeForExpression(node);
			if (cnode != null) {
				BitVectorVariable ins = solver.getIn(cnode);

				BitVectorVariable refNodes = modRef.getRef(node);

				result = intersect(refNodes, ins);
			}
		}

		public void visitParameter(AbstractParameterNode node) {
			result = emptySet;

			if (node.isException() || node.isExit()) {
				return;
			}

			CFGNode cnode = ecfg.getNodeForParameter(node);
			if (cnode != null) {
				/*
				 * 1. retrieve the set of all definitions that are visible at the
				 * current node.
				 *
				 * 2. look for all nodes in this set that may be references by
				 * the current node.
				 */

				BitVectorVariable ins = solver.getIn(cnode);

				BitVectorVariable refNodes = modRef.getRef(node);

				result = intersect(refNodes, ins);
			}
		}

		public void visitNormal(NormalNode node) {
			result = emptySet;

			if (node.isHeapCompound()) {
				HeapAccessCompound hacc = (HeapAccessCompound) node;
				CFGNode cnode = ecfg.getNodeForHeapAccess(hacc);
				if (cnode != null) {
					BitVectorVariable ins = solver.getIn(cnode);

					BitVectorVariable refNodes = modRef.getRef(node);

					result = intersect(refNodes, ins);
				}
			}
		}

		public void visitCatch(CatchNode node) {}
		public void visitConstPhiValue(ConstantPhiValueNode node) {}
		public void visitPredicate(PredicateNode node) {}
		public void visitSync(SyncNode node) {}
		public void visitPhiValue(PhiValueNode node) {}

	}

	/**
	 * Compute set of nodes that may def the heapvalue read by statement node
	 * @param node node that (possibly) reads heapstatements
	 * @return set of possible last def nodes
	 */
	private OrdinalSet<AbstractPDGNode> computeLastDefForNode(AbstractPDGNode node,
			final BitVectorSolver<CFGNode> solver) {
		LastDefVisitor lastDefVisitor = new LastDefVisitor(solver);

		node.accept(lastDefVisitor);

		return lastDefVisitor.getResult();
	}

	public class RelevantNodesVisitor implements IPDGNodeVisitor {
		/**
		 * we accept all nodes as relevant ones for now. this may be
		 * optimized in the future.
		 */

		private boolean accept = false;

		public boolean isLastAccepted() {
			return accept;
		}

		/**
		 * No form-in/out nodes from other pdgs are accepted.
		 * Non-static root nodes are also not accepted as their dataflow
		 * has already been treated.
		 */
		public void visitParameter(AbstractParameterNode node) {
			// only accept form in/out nodes that belong to this pdg
			// form-in/out nodes from other pdgs may have been referenced
			// in this pdg because they were attached to act-in/out nodes
			// of a call instruction
//			accept = node.isExit() || (!node.isException() &&
//				(node.getPdgId() == pdg.getId()) && node.isOnHeap());
			accept = (node.getPdgId() == pdg.getId()) && node.isOnHeap();
		}

		public void visitCall(CallNode node) {
			accept = true;
		}

		public void visitEntry(EntryNode node) {
			accept = true;
		}

		public void visitExpression(ExpressionNode node) {
			// only field get and set instructions are relevant for our
			// heap dataflow analysis
			accept = node.isFieldAccess() || node.isArrayAccess();
		}

		public void visitNormal(NormalNode node) {
			accept = node.isHeapCompound();
		}

		public void visitPredicate(PredicateNode node) {
			accept = false;
		}

		public void visitSync(SyncNode node) {
			accept = true;
		}

		public void visitCatch(CatchNode node) {
			accept = false;
		}

		public void visitConstPhiValue(ConstantPhiValueNode node) {
			accept = false;
		}

		public void visitPhiValue(PhiValueNode node) {
			accept = false;
		}


	}

	/**
	 * Filter all nodes that should be used in dataflow analysis. Some nodes are
	 * left out. Like for example formal-in/out nodes belonging to another pdg.
	 * These are sorted out through their pdg id. we are only intrested in nodes
	 * directly belonging to this pdg.
	 *
	 * For the filtered nodes a new mapping is generated so that they can
	 * be used by the generic dataflow framework.
	 */
	private void prepareStatements(IProgressMonitor monitor) throws CancelException {
		/**
		 * CAUTION form-in/out nodes not contained in the lists above are from
		 * other pdgs (and connected to act-in/out nodes of this pdg).
		 * We have to ignore them in our dataflow analysis.
		 * Nodes in the pdg that are references from other pdgs can be identified
		 * by their id field. Their id does not match the id of the current pdg.
		 *
		 * Other nodes we ignore are the ones that do not affect Heapvalues.
		 * Which means that they do not write to nor read from static/dynamic
		 * fields.
		 */

		Predicate<AbstractPDGNode> filter = new Predicate<AbstractPDGNode>() {

			private RelevantNodesVisitor acceptRelevantNodesVisitor =
				new RelevantNodesVisitor();

			@Override
			public boolean test(AbstractPDGNode o) {
				o.accept(acceptRelevantNodesVisitor);

				return o.getPdgId() == pdg.getId() &&
					acceptRelevantNodesVisitor.isLastAccepted();
			}

		};

		FilterIterator<AbstractPDGNode> filterIt =
			new FilterIterator<AbstractPDGNode>(pdg.iterator(), filter);

		statements = Iterator2Collection.toList(filterIt);

		mapping = createDomain(statements);

		/*
		 * create mod/ref stuff
		 */

		modRef = pdg.getParamModel().getModRef(mapping);
		modRef.computeModRef(monitor);
	}

	private static OrdinalSetMapping<AbstractPDGNode> createDomain(Collection<AbstractPDGNode> coll) {
		AbstractPDGNode[] arr = new AbstractPDGNode[coll.size()];
		OrdinalSetMapping<AbstractPDGNode> domain =
			new ObjectArrayMapping<AbstractPDGNode>(coll.toArray(arr));

		return domain;
	}

}

