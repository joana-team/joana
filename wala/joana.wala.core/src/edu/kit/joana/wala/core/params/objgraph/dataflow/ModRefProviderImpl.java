/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.dataflow;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.wala.core.params.objgraph.ModRefFieldCandidate;
import edu.kit.joana.wala.core.params.objgraph.dataflow.ModRefControlFlowGraph.Node;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class ModRefProviderImpl implements ModRefProvider {

	private final Map<Node, IntSet> node2mustMod = new HashMap<Node, IntSet>();
	private final Map<Node, IntSet> node2mayMod = new HashMap<Node, IntSet>();
	private final Map<Node, IntSet> node2mayRef = new HashMap<Node, IntSet>();

	private ModRefProviderImpl(boolean isParallel) {
		this.isParallel = isParallel;
	}
	
	private final boolean isParallel;

	public static OrdinalSetMapping<Node> createDomain(final ModRefControlFlowGraph cfg) {
		final List<Node> list = new LinkedList<ModRefControlFlowGraph.Node>();
		for (final Node n : cfg) {
			if (!n.isNOP()) {
				list.add(n);
			}
		}

		final Node[] nodes = list.toArray(new Node[list.size()]);

		return new ObjectArrayMapping<ModRefControlFlowGraph.Node>(nodes);
	}

	public static ModRefProvider createProvider(final ModRefControlFlowGraph cfg, final OrdinalSetMapping<Node> domain, final boolean isParallel) {
		final ModRefProviderImpl modRef = new ModRefProviderImpl(isParallel);

		modRef.run(cfg, domain);

		return modRef;
	}

	private void run(final ModRefControlFlowGraph cfg, final OrdinalSetMapping<Node> domain) {
		// for formal-in/-out nodes mod and ref has to be exchanged
		// this is done by the isMod() isRef() methods of Node. Do not use ModRefCandidate isMod()
		// isRef() here.

		StreamSupport.stream(domain.spliterator(), this.isParallel)
			.forEach(n->calc(n, cfg, domain));
	}
	
	private void calc(Node n, final ModRefControlFlowGraph cfg,
						final OrdinalSetMapping<Node> domain) {
		final ModRefFieldCandidate nCand = n.getCandidate();

		final BitVector bvMustMod = new BitVector();
		final BitVector bvMayMod = new BitVector();
		final BitVector bvMayRef = new BitVector();
		final boolean isMod = n.isMod();
		final boolean isRef = n.isRef();

		for (final Node other : domain) {

			final ModRefFieldCandidate otherCand = other.getCandidate();
			final int id = domain.getMappedIndex(other);

			if (isMod && other.isRef()) {
				if (n == other) {
					bvMayMod.set(id);
					bvMustMod.set(id);
				} else if (nCand.isMustAliased(otherCand)) {
					bvMayMod.set(id);
					bvMustMod.set(id);
				} else if (nCand.isMayAliased(otherCand)) {
					bvMayMod.set(id);
				}
			}

			if (isRef && other.isMod()) {
				if (n == other) {
					bvMayRef.set(id);
				} else if (nCand.isMayAliased(otherCand)) {
					bvMayRef.set(id);
				}
			}
		}
		
		putResult(n, bvMustMod, bvMayMod, bvMayRef);
	}
	
	private synchronized void putResult(Node n,
				BitVector bvMustMod, BitVector bvMayMod,BitVector bvMayRef) {
		node2mustMod.put(n, new BitVectorIntSet(bvMustMod));
		node2mayMod.put(n, new BitVectorIntSet(bvMayMod));
		node2mayRef.put(n, new BitVectorIntSet(bvMayRef));
	}

	@Override
	public IntSet getMustMod(final Node node) {
		return node2mustMod.get(node);
	}

	@Override
	public IntSet getMayMod(final Node node) {
		return node2mayMod.get(node);
	}

	@Override
	public IntSet getMayRef(final Node node) {
		return node2mayRef.get(node);
	}

}
