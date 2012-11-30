/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params;

import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorIdentity;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.BitVectorUnionVector;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SparseNumberedGraph;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGNode;

public class FlatHeapParamsDataFlow<T> {

	private final Map<PDGNode, OrdinalSet<T>> node2ptsMod;
	private final Map<PDGNode, OrdinalSet<T>> node2ptsRef;
	private final OrdinalSetMapping<PDGNode> mapping;

	private FlatHeapParamsDataFlow(Graph<PDGNode> flow, Map<PDGNode, OrdinalSet<T>> node2ptsMod, Map<PDGNode, OrdinalSet<T>> node2ptsRef,
			OrdinalSetMapping<PDGNode> mapping) {
		this.node2ptsMod = node2ptsMod;
		this.node2ptsRef = node2ptsRef;
		this.mapping = mapping;
	}

	public static <T> void compute(PDG pdg, Map<PDGNode, OrdinalSet<T>> node2ptsMod, Map<PDGNode,
			OrdinalSet<T>> node2ptsRef, PDGEdge.Kind edgeKind, IProgressMonitor monitor) throws CancelException {
		final MutableMapping<PDGNode> mapping = MutableMapping.make();
		final SparseNumberedGraph<PDGNode> flow = new SparseNumberedGraph<PDGNode>();
		for (final PDGNode node : pdg.vertexSet()) {
			if (node.getPdgId() == pdg.getId()) {
				mapping.add(node);
				flow.addNode(node);
			}
		}

		for (final PDGNode node : mapping) {
			for (final PDGEdge out : pdg.outgoingEdgesOf(node)) {
				if (out.kind == PDGEdge.Kind.CONTROL_FLOW || out.kind == PDGEdge.Kind.CONTROL_FLOW_EXC) {
					flow.addEdge(node, out.to);
				}
			}
		}

		final FlatHeapParamsDataFlow<T> df = new FlatHeapParamsDataFlow<T>(flow, node2ptsMod, node2ptsRef, mapping);
		final ITransferFunctionProvider<PDGNode, BitVectorVariable> transfer = df.makeTransfer();
		final BitVectorFramework<PDGNode, PDGNode> framework = new BitVectorFramework<PDGNode, PDGNode>(flow, transfer, mapping);

		final BitVectorSolver<PDGNode> solver = new BitVectorSolver<PDGNode>(framework);
		solver.solve(monitor);

		final Map<PDGNode, OrdinalSet<PDGNode>> node2source = df.buildResult(solver);

		for (final PDGNode to : node2source.keySet()) {
			final OrdinalSet<PDGNode> fromNodes = node2source.get(to);

			for (final PDGNode from : fromNodes) {
				pdg.addEdge(from, to, edgeKind);
			}
		}
	}

	private Map<PDGNode, OrdinalSet<PDGNode>> buildResult(BitVectorSolver<PDGNode> solver) {
		final Map<PDGNode, OrdinalSet<PDGNode>> result = new HashMap<PDGNode, OrdinalSet<PDGNode>>();

		for (final PDGNode node : mapping) {
			if (node2ptsRef.containsKey(node)) {
				final BitVectorIntSet from = new BitVectorIntSet();
				final BitVectorVariable bvIn = solver.getIn(node);
				final OrdinalSet<T> ptsRef = node2ptsRef.get(node);
				final OrdinalSet<PDGNode> reaching = new OrdinalSet<PDGNode>(bvIn.getValue(), mapping);

				for (final PDGNode reach : reaching) {
					if (node2ptsMod.containsKey(reach)) {
						final OrdinalSet<T> ptsMod = node2ptsMod.get(reach);
						if (ptsMod.containsAny(ptsRef)) {
							// add data dep
							final int id = mapping.add(reach);
							from.add(id);
						}
					}
				}

				final OrdinalSet<PDGNode> fromNodes = new OrdinalSet<PDGNode>(from, mapping);
				result.put(node, fromNodes);
			}
		}

		return result;
	}



	private TransferFunctionProvider makeTransfer() {
		return new TransferFunctionProvider();
	}

	private class TransferFunctionProvider implements ITransferFunctionProvider<PDGNode, BitVectorVariable> {

		@Override
		public UnaryOperator<BitVectorVariable> getNodeTransferFunction(PDGNode node) {
			if (node2ptsMod.containsKey(node) || node2ptsRef.containsKey(node)) {
				final int id = mapping.getMappedIndex(node);
				final BitVector bv = new BitVector();
				bv.set(id);
				return new BitVectorUnionVector(bv);
			} else {
				return BitVectorIdentity.instance();
			}
		}

		@Override
		public boolean hasNodeTransferFunctions() {
			return true;
		}

		@Override
		public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(PDGNode src, PDGNode dst) {
			return null;
		}

		@Override
		public boolean hasEdgeTransferFunctions() {
			return false;
		}

		@Override
		public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
			return BitVectorUnion.instance();
		}

	}

}
