/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.dataflow;

import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.ParameterField;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class ModRefStaticField implements IModRef {

	private final OrdinalSetMapping<PDGNode> nodes;
	private final TIntObjectMap<BitVectorVariable> nodeid2ref = new TIntObjectHashMap<BitVectorVariable>();
	private final TIntObjectMap<BitVectorVariable> nodeid2mod = new TIntObjectHashMap<BitVectorVariable>();
	private boolean isComputed = false;
	private Map<PDGNode, ParameterField> access; // not final because the reference is removed after the computation.

	private static final BitVectorVariable EMPTY = new BitVectorVariable();

	public ModRefStaticField(OrdinalSetMapping<PDGNode> nodes, Map<PDGNode, ParameterField> access) {
		this.nodes = nodes;
		this.access = access;
	}

	@Override
	public void compute(IProgressMonitor monitor) throws CancelException {
		if (!isComputed) {
			Map<ParameterField, BitVectorVariable> reads = new HashMap<ParameterField, BitVectorVariable>();
			Map<ParameterField, BitVectorVariable> writes = new HashMap<ParameterField, BitVectorVariable>();

			for (PDGNode node : nodes) {
				ParameterField field = access.get(node);
				if (field == null || !field.isStatic()) {
					continue;
				}

				switch (node.getKind()) {
				case ACTUAL_IN:
				case HREAD:
				case FORMAL_OUT: {
					BitVectorVariable v = reads.get(field);
					if (v == null) {
						v = new BitVectorVariable();
						reads.put(field, v);
					}
					v.set(nodes.getMappedIndex(node));
				} break;
				case ACTUAL_OUT:
				case HWRITE:
				case FORMAL_IN: {
					BitVectorVariable v = writes.get(field);
					if (v == null) {
						v = new BitVectorVariable();
						writes.put(field, v);
					}
					v.set(nodes.getMappedIndex(node));
				} break;
				default:
					throw new IllegalStateException("Don't know what to do with: " + node);
				}
			}

			for (PDGNode node : nodes) {
				ParameterField field = access.get(node);
				if (field == null || !field.isStatic()) {
					continue;
				}

				final int nodeId = nodes.getMappedIndex(node);

				switch (node.getKind()) {
				case FORMAL_OUT:
				case HREAD:
				case ACTUAL_IN: {
					// read access
					BitVectorVariable ref = nodeid2ref.get(nodeId);
					if (ref == null) {
						ref = new BitVectorVariable();
						nodeid2ref.put(nodeId, ref);
					}

					BitVectorVariable w = writes.get(field);
					if (w != null) {
						ref.addAll(w);
					}
				} break;
				case ACTUAL_OUT:
					// actual out may or may not modify other values..
					break;
				case FORMAL_IN:
				case HWRITE: {
					BitVectorVariable mod = nodeid2mod.get(nodeId);
					if (mod == null) {
						mod = new BitVectorVariable();
						nodeid2mod.put(nodeId, mod);
					}

					BitVectorVariable r = reads.get(field);
					if (r != null) {
						mod.addAll(r);
					}
				} break;
				default: // nothing to do here
				}
			}

			access = null;
			isComputed = true;
		}
	}

	@Override
	public BitVectorVariable getMod(PDGNode node) {
		if (!isComputed) {
			throw new IllegalStateException("Run compute first!");
		}

		final int id = nodes.getMappedIndex(node);

		if (!nodeid2mod.containsKey(id)) {
			return EMPTY;
		} else {
			return nodeid2mod.get(id);
		}
	}

	@Override
	public BitVectorVariable getRef(PDGNode node) {
		if (!isComputed) {
			throw new IllegalStateException("Run compute first!");
		}

		final int id = nodes.getMappedIndex(node);

		if (!nodeid2ref.containsKey(id)) {
			return EMPTY;
		} else {
			return nodeid2ref.get(id);
		}
	}

}
