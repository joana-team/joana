/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.dataflow;

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

import edu.kit.joana.wala.core.PDGNode;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ReachingDefsTransferFP implements ITransferFunctionProvider<PDGNode, BitVectorVariable> {

	private final OrdinalSetMapping<PDGNode> domain;
	private final IModRef modRef;

	public ReachingDefsTransferFP(OrdinalSetMapping<PDGNode> domain, IModRef modRef) {
		this.domain = domain;
		this.modRef = modRef;
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
	public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(PDGNode srcNode, PDGNode dstNode) {
//		if (!isInNormalSuccessors(srcNode, dstNode)) {
//			// if the edge only happens due to exceptional control flow, then no
//			// heap locations
//			// are def'ed or used
//			return BitVectorIdentity.instance();
//		} else {
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
//		}
	}

	/**
	 * Computes the KILL set for an ExplodedBasicBlock. The returned BitVector
	 * has a bit for each node in the PDG. The index of each bit can be accessed
	 * through the OridnalSetMapping domain.
	 * @param node a node of the control flow graph
	 * @return kill set of pdg nodes as bitvector
	 */
	public BitVector kill(PDGNode node) {
		// default assign empty bitvector as no nodes are killed by this statement
		BitVector result = emptyVector;

		BitVectorVariable bvMod = modRef.getMod(node);

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
	public IntSet gen(PDGNode node) {
		IntSet result = emptySet;

		if (domain.hasMappedIndex(node)) {
			int id = domain.getMappedIndex(node);

			assert (id >= 0) : "No id in mapping for: " + node;

			result = SparseIntSet.singleton(id);
		}

		return result;
	}

	private final static IntSet emptySet = null;

	public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
		return BitVectorUnion.instance();
	}

	public UnaryOperator<BitVectorVariable> getNodeTransferFunction(PDGNode node) {
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

