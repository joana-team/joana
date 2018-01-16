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
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntIterator;
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

	public static ModRefProvider createProvider(final OrdinalSetMapping<Node> domain, final boolean isParallel) {
		final ModRefProviderImpl modRef = new ModRefProviderImpl(isParallel);

		modRef.run(domain);

		return modRef;
	}

	private void run(final OrdinalSetMapping<Node> domain) {
		// for formal-in/-out nodes mod and ref has to be exchanged
		// this is done by the isMod() isRef() methods of Node. Do not use ModRefCandidate isMod()
		// isRef() here.

		StreamSupport.stream(domain.spliterator(), this.isParallel)
			.forEach(n->calc(n, domain));
	}
	
	private BitVector calcSlow(Node n, final OrdinalSetMapping<Node> domain) {
		final ModRefFieldCandidate nCand = n.getCandidate();

		final BitVector bvMustMod = new BitVector();
		final boolean isMod = n.isMod();

		for (final Node other : domain) {

			final ModRefFieldCandidate otherCand = other.getCandidate();
			final int id = domain.getMappedIndex(other);

			if (isMod && other.isRef()) {
				if (n == other) {
					bvMustMod.set(id); // TODO: why is this even sound?
				} else if (nCand.isMustAliased(otherCand)) {
					bvMustMod.set(id);
				}
			}
		}
		
		return bvMustMod;
	}
	
	private void calc(Node n, final OrdinalSetMapping<Node> domain) {
		final ModRefFieldCandidate nCand = n.getCandidate();

		final BitVector bvMustMod = new BitVector();
		final boolean isMod = n.isMod();
		
		if (isMod) {
			if (n.isRef()) {
				bvMustMod.set(domain.getMappedIndex(n)); // TODO: why is this even sound?
			}
			if (nCand.canMustAlias()) {
				for (final Node other : domain) {
					if (n != other && other.isRef()) {
						final ModRefFieldCandidate otherCand = other.getCandidate();
						if (nCand.isMustAliased(otherCand)) {
							final int id = domain.getMappedIndex(other);
							bvMustMod.set(id);
						}
					}
				}
			}
			
		}
		
		assert bvMustMod.sameBits(calcSlow(n, domain));
		
		putResult(n, bvMustMod);
	}
	
	private synchronized void putResult(Node n,
				BitVector bvMustMod) {
		node2mustMod.put(n, new BitVectorIntSet(bvMustMod));
	}

	@Override
	public IntSet getMustMod(final Node node) {
		return node2mustMod.get(node);
	}

	public IntSet getMayRef(final Node n, IntSet inNodes, final OrdinalSetMapping<Node> mapping) {
		final BitVector bvMayRef = new BitVector();
		final boolean isRef = n.isRef();
		
		if (!isRef) {
			return new EmptyIntSet();
		}

		final IntIterator it = inNodes.intIterator();
		final ModRefFieldCandidate nCand = n.getCandidate();
		
		while (it.hasNext()) {
			final int id = it.next();
			final Node other = mapping.getMappedObject(id);
			assert isRef;
			if (other.isMod()) {
				final ModRefFieldCandidate otherCand = other.getCandidate();
				if (n == other) {
					bvMayRef.set(id);
				} else if (nCand.isMayAliased(otherCand)) {
					bvMayRef.set(id);
				}
			}
			
		}
		final IntSet result = new BitVectorIntSet(bvMayRef);
		return result;
	}

}
