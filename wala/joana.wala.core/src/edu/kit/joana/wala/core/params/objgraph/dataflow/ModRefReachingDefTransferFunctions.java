/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.dataflow;

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

import edu.kit.joana.wala.core.params.objgraph.dataflow.ModRefControlFlowGraph.Node;

public final class ModRefReachingDefTransferFunctions
implements ITransferFunctionProvider<ModRefControlFlowGraph.Node, BitVectorVariable> {

	private final OrdinalSetMapping<Node> domain;
	private final ModRefProvider modRef;

	public ModRefReachingDefTransferFunctions(final OrdinalSetMapping<Node> domain, final ModRefProvider modRef) {
		this.domain = domain;
		this.modRef = modRef;
	}

	@Override
	public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(final Node src, final Node dst) {
        final BitVector kill = kill(src);
        final IntSet gen = gen(src);

        if (kill == null && gen == null) {
            return BitVectorIdentity.instance();
        } else if (kill == null && gen != null) {
            return new BitVectorUnionVector(new BitVectorIntSet(gen).getBitVector());
        } else if (kill != null && gen == null) {
            return new BitVectorMinusVector(kill);
        } else if (kill != null && gen != null) {
            return new BitVectorKillGen(kill, new BitVectorIntSet(gen).getBitVector());
        } else {
        	// this else part should never be executed as the if clauses
        	// should be complete
        	throw new IllegalStateException();
        }
	}

	public BitVector kill(final Node node) {
		// default assign empty bitvector as no nodes are killed by this statement
		final IntSet modSet = modRef.getMustMod(node);

		if (modSet != null && !modSet.isEmpty()) {
			return new BitVectorIntSet(modSet).getBitVector();
		} else {
			return emptyVector;
		}
	}

	private final static BitVector emptyVector = null;

	public IntSet gen(final Node node) {
		if (domain.hasMappedIndex(node)) {
			final int id = domain.getMappedIndex(node);
			assert (id >= 0) : "No id in mapping for: " + node;

			return SparseIntSet.singleton(id);
		} else {
			return emptySet;
		}
	}

	private final static IntSet emptySet = null;

	@Override
	public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
		return BitVectorUnion.instance();
	}

	@Override
	public UnaryOperator<BitVectorVariable> getNodeTransferFunction(final Node node) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasEdgeTransferFunctions() {
		return true;
	}

	@Override
	public boolean hasNodeTransferFunctions() {
		return false;
	}


}
