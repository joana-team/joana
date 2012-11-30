/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.dataflow;


import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorIdentity;
import com.ibm.wala.dataflow.graph.BitVectorKillGen;
import com.ibm.wala.dataflow.graph.BitVectorMinusVector;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.BitVectorUnionVector;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;
import com.ibm.wala.util.intset.SparseIntSet;

import edu.kit.joana.deprecated.jsdg.sdg.dataflow.CFGWithParameterNodes.CFGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ExpressionNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IModRef;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ReachingDefsTransferFP implements
ITransferFunctionProvider<CFGNode, BitVectorVariable> {

	private final CFGWithParameterNodes ecfg;
	private final OrdinalSetMapping<AbstractPDGNode> domain;
	private final IModRef modRef;

	public ReachingDefsTransferFP(CFGWithParameterNodes ecfg, OrdinalSetMapping<AbstractPDGNode> domain, IModRef modRef) {
		this.ecfg = ecfg;
		this.domain = domain;
		this.modRef = modRef;
	}

	/**
	 * Used to check if two cfg nodes are reachable through a normal
	 * (non-exceptional) path
	 * @param src cfg node
	 * @param dst cfg node
	 * @return true if dst is reachable through a non-exceptional path
	 */
	private boolean isInNormalSuccessors(CFGNode src, CFGNode dst) {
		return ecfg.isInNormalSuccessors(src, dst);
	}

	/**
	 * Computes the edge transfer function for aech cfg edge using the kill
	 * and gen sets. Kill sets are represented as bitvectors (as they may contain
	 * many entries) and gen sets as simple intsets (as they usually contain very
	 * few entries - mostly a single entry)
	 * @param srcNode cfg node that is source of the edge
	 * @param dstNode cfg node that is the destination of the cfg edge
	 * @return A transferfunction on bitvectors modelling the effect on the last
	 * reaching defs this cfg edge has
	 */
	public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(CFGNode srcNode, CFGNode dstNode) {
		if (!isInNormalSuccessors(srcNode, dstNode)) {
			// if the edge only happens due to exceptional control flow, then no
			// heap locations
			// are def'ed or used
			return BitVectorIdentity.instance();
		} else {
	        BitVector kill = kill(srcNode);
	        IntSet gen = gen(srcNode);
	        if (kill == null && gen == null) {
	            return BitVectorIdentity.instance();
	        } else if (kill == null && gen != null) {
	            return new BitVectorUnionVector(new BitVectorIntSet(gen).getBitVector());
	        } else if (kill != null && gen == null) {
	            return new BitVectorMinusVector(kill);
	        } else if (kill != null && gen != null) {
	            return new BitVectorKillGen(kill, new BitVectorIntSet(gen).getBitVector());
	        } else {
	        	// this else part should never be excecuted as the if clauses
	        	// should be complete
	        	throw new IllegalStateException();
	        }
		}
	}

	/**
	 * Computes the KILL set for an ExplodedBasicBlock. The returned BitVector
	 * has a bit for each node in the PDG. The index of each bit can be accessed
	 * through the OridnalSetMapping domain.
	 * @param node a node of the control flow graph
	 * @return kill set of pdg nodes as bitvector
	 */
	public BitVector kill(CFGNode node) {
		// default assign empty bitvector as no nodes are killed by this statement
		BitVector result = emptyVector;

		BitVectorVariable bvMod;
		if (node.isBasicBlock()) {
			AbstractPDGNode pnode = ecfg.getMainPDGNode(node.getBasicBlock());

			if (pnode == null) {
				return emptyVector;
			}

			bvMod = modRef.getMod(pnode);
		} else if (node.isParameter()){
			AbstractParameterNode param = node.getParameterNode();

			if (param.isOnHeap()) {
				bvMod = modRef.getMod(param);
			} else {
				return emptyVector;
			}
		} else if (node.isCall()) {
			CallNode call = node.getCall();

			bvMod = modRef.getMod(call);
		} else if (node.isArtificial()) {
			AbstractPDGNode pdgNode = node.getArtificialNode();

			bvMod = modRef.getMod(pdgNode);
		} else {
			throw new IllegalStateException();
		}

		IntSet modSet = bvMod.getValue();
		if (modSet != null && !modSet.isEmpty()) {
			result = new BitVectorIntSet(modSet).getBitVector();
		}

		return result;
	}

	private final static BitVector emptyVector = null;

	/**
	 * Computes a set of pdg nodes whose value is defined by a given cfg node.
	 *
	 * In this implementation these sets contain at most one element and are
	 * usually empty. Only cfg nodes of field-set operations and formal-in /
	 * actual-out nodes define a new value -> the sdg node they correspond to.
	 *
	 * @param node node of the control flow graph
	 * @return set of pdg nodes whose values have been generated
	 */
	public IntSet gen(CFGNode node) {
		IntSet result = emptySet;

		/*
		 * When the cfg node is a normal basic block we look for field-set
		 * operations that define a value.
		 *
		 * Else the cfg node represents a parameter node and we look for either
		 * a formal-in or an actual-out node.
		 *
		 * All other cfg nodes do not define a new heap value, so the empty set
		 * is used for them.
		 */
		if (node.isBasicBlock()) {
			AbstractPDGNode pnode = ecfg.getMainPDGNode(node.getBasicBlock());

			if (pnode instanceof ExpressionNode) {
				ExpressionNode expr = (ExpressionNode) pnode;

				if (expr.isSet() && (expr.isFieldAccess() || expr.isArrayAccess())) {
					int id = domain.getMappedIndex(pnode);

					assert (id >= 0) : "No id in mapping for: " + pnode;

					result = SparseIntSet.singleton(id);
				}
			}
		} else if (node.isParameter()) {
			AbstractParameterNode param = node.getParameterNode();

			if (param.isOnHeap() && ((param.isIn() && param.isFormal())
				|| (param.isOut() && param.isActual()))) {

				int id = domain.getMappedIndex(param);

				assert (id >= 0) : "No id in mapping for: " + param;

				result = SparseIntSet.singleton(id);
			}
		} else if (node.isCall()) {
			// nothing to do for a call
			// a call does not define or kill stuff -> only its parameter nodes
		} else if (node.isArtificial()) {
			AbstractPDGNode pnode = node.getArtificialNode();

			if (pnode instanceof ExpressionNode) {
				ExpressionNode expr = (ExpressionNode) pnode;

				if (expr.isSet() && (expr.isFieldAccess() || expr.isArrayAccess())) {
					int id = domain.getMappedIndex(pnode);

					assert (id >= 0) : "No id in mapping for: " + pnode;

					result = SparseIntSet.singleton(id);
				}
			}
		} else {
			throw new IllegalStateException();
		}

		return result;
	}

	private final static IntSet emptySet = null;

	public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
		return BitVectorUnion.instance();
	}

	public UnaryOperator<BitVectorVariable> getNodeTransferFunction(CFGNode node) {
		// UNREACHABLE
		assert false;

		return null;
	}

	public boolean hasEdgeTransferFunctions() {
		return true;
	}

	public boolean hasNodeTransferFunctions() {
		return false;
	}

}

