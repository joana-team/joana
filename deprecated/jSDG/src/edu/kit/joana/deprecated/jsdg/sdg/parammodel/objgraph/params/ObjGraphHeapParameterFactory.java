/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params;

import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;

/**
 * Factory that guarantees that heap parameters are only created when no
 * similar node already exists. This helps to reduce the overall number of parameters.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ObjGraphHeapParameterFactory {

	private final Map<ParameterField, Set<ActInHeapNode>> actInHeap;
	private final Map<ParameterField, Set<ActInHeapNode>> actInStatic;
	private final Map<ParameterField, Set<ActOutHeapNode>> actOutHeap;
	private final Map<ParameterField, Set<ActOutHeapNode>> actOutStatic;
	private final Map<ParameterField, Set<FormInHeapNode>> formInHeap;
	private final Map<ParameterField, Set<FormInHeapNode>> formInStatic;
	private final Map<ParameterField, Set<FormOutHeapNode>> formOutHeap;
	private final Map<ParameterField, Set<FormOutHeapNode>> formOutStatic;

	public ObjGraphHeapParameterFactory() {
		this.actInHeap = HashMapFactory.make();
		this.actInStatic = HashMapFactory.make();
		this.actOutHeap = HashMapFactory.make();
		this.actOutStatic = HashMapFactory.make();
		this.formInHeap = HashMapFactory.make();
		this.formInStatic = HashMapFactory.make();
		this.formOutHeap = HashMapFactory.make();
		this.formOutStatic = HashMapFactory.make();
	}

	private final static <T extends ObjGraphParameter> Set<T> findSet(Map<ParameterField, Set<T>> map, ParameterField field) {
		Set<T> set = map.get(field);
		if (set == null) {
			set = HashSetFactory.make();
			map.put(field, set);
		}

		return set;
	}

	private static final boolean setsAreEqual(OrdinalSet<InstanceKey> set1, OrdinalSet<InstanceKey> set2) {
		return ((set1 == null && set2 == null) ||
				(set1 != null && set2 != null && set1.isEmpty() && set2.isEmpty()) ||
				(set1 != null && set2 != null && !set1.isEmpty() && !set2.isEmpty() &&
						set1.getBackingSet().sameValue(set2.getBackingSet())));
	}

	private static final <T extends FormNode> T findNode(Set<T> nodes, OrdinalSet<InstanceKey> basePts, int pdgId) {
		for (T n : nodes) {
			if (n.getPdgId() == pdgId && setsAreEqual(n.getBasePointsTo(), basePts)) {
				return n;
			}
		}

		return null;
	}

	private static final <T extends FormNode> T findNode(Set<T> nodes, int pdgId) {
		for (T n : nodes) {
			if (n.getPdgId() == pdgId) {
				return n;
			}
		}

		return null;
	}

	private static final <T extends ActNode> T findNode(Set<T> nodes, OrdinalSet<InstanceKey> basePts, int pdgId, CallNode call) {
		for (T n : nodes) {
			if (n.getPdgId() == pdgId && n.getCallId() == call.getUniqueId() && setsAreEqual(n.getBasePointsTo(), basePts)) {
				return n;
			}
		}

		return null;
	}

	private static final <T extends ActNode> T findNode(Set<T> nodes, int pdgId, CallNode call) {
		for (T n : nodes) {
			if (n.getPdgId() == pdgId && n.getCallId() == call.getUniqueId()) {
				return n;
			}
		}

		return null;
	}

	public ActInHeapNode makeActInHeap(CallNode call, OrdinalSet<InstanceKey> basePts, ParameterField baseField, int id,
			boolean isPrimitive, TypeReference type, Set<PointerKey> pKey, OrdinalSet<InstanceKey> pts) {
		Set<ActInHeapNode> aIns = findSet(actInHeap, baseField);
		ActInHeapNode aIn = findNode(aIns, basePts, id, call);

		if (aIn == null) {
			aIn = new ActInHeapNode(basePts, baseField, id, isPrimitive, type, pKey, pts, call.getUniqueId());
			aIns.add(aIn);
		}

		return aIn;
	}

	public ActInHeapNode makeActInStatic(CallNode call, ParameterField baseField, int id,
			boolean isPrimitive, TypeReference type, Set<PointerKey> pKey, OrdinalSet<InstanceKey> pts) {
		assert (baseField.isStatic());

		Set<ActInHeapNode> aIns = findSet(actInStatic, baseField);
		ActInHeapNode aIn = findNode(aIns, id, call);
		if (aIn == null) {
			aIn = new ActInHeapNode(baseField, id, isPrimitive, type, pKey, pts, call.getUniqueId());
			aIns.add(aIn);
		}

		return aIn;
	}

	public ActOutHeapNode makeActOutHeap(CallNode call, OrdinalSet<InstanceKey> basePts, ParameterField baseField, int id,
			boolean isPrimitive, TypeReference type, Set<PointerKey> pKey, OrdinalSet<InstanceKey> pts) {
		Set<ActOutHeapNode> aOuts = findSet(actOutHeap, baseField);
		ActOutHeapNode aOut = findNode(aOuts, basePts, id, call);

		if (aOut == null) {
			aOut = new ActOutHeapNode(basePts, baseField, id, isPrimitive, type, pKey, pts, call.getUniqueId());
			aOuts.add(aOut);
		}

		return aOut;
	}

	public ActOutHeapNode makeActOutStatic(CallNode call, ParameterField baseField, int id,
			boolean isPrimitive, TypeReference type, Set<PointerKey> pKey, OrdinalSet<InstanceKey> pts) {
		assert (baseField.isStatic());

		Set<ActOutHeapNode> aOuts = findSet(actOutStatic, baseField);
		ActOutHeapNode aOut = findNode(aOuts, id, call);
		if (aOut == null) {
			aOut = new ActOutHeapNode(baseField, id, isPrimitive, type, pKey, pts, call.getUniqueId());
			aOuts.add(aOut);
		}

		return aOut;
	}


	public FormInHeapNode makeFormInHeap(OrdinalSet<InstanceKey> basePts, ParameterField baseField, int id,
			boolean isPrimitive, TypeReference type, Set<PointerKey> pKey, OrdinalSet<InstanceKey> pts) {
		Set<FormInHeapNode> fIns = findSet(formInHeap, baseField);
		FormInHeapNode fIn = findNode(fIns, basePts, id);

		if (fIn == null) {
			fIn = new FormInHeapNode(basePts, baseField, id, isPrimitive, type, pKey, pts);
			fIns.add(fIn);
		}

		return fIn;
	}

	public FormInHeapNode makeFormInStatic(ParameterField baseField, int id,
			boolean isPrimitive, TypeReference type, Set<PointerKey> pKey, OrdinalSet<InstanceKey> pts) {
		assert (baseField.isStatic());

		Set<FormInHeapNode> fIns = findSet(formInStatic, baseField);
		FormInHeapNode fIn = findNode(fIns, id);
		if (fIn == null) {
			fIn = new FormInHeapNode(baseField, id, isPrimitive, type, pKey, pts);
			fIns.add(fIn);
		}

		return fIn;
	}

	public FormOutHeapNode makeFormOutHeap(OrdinalSet<InstanceKey> basePts, ParameterField baseField, int id,
			boolean isPrimitive, TypeReference type, Set<PointerKey> pKey, OrdinalSet<InstanceKey> pts) {
		Set<FormOutHeapNode> fOuts = findSet(formOutHeap, baseField);
		FormOutHeapNode fOut = findNode(fOuts, basePts, id);

		if (fOut == null) {
			fOut = new FormOutHeapNode(basePts, baseField, id, isPrimitive, type, pKey, pts);
			fOuts.add(fOut);
		}

		return fOut;
	}

	public FormOutHeapNode makeFormOutStatic(ParameterField baseField, int id,
			boolean isPrimitive, TypeReference type, Set<PointerKey> pKey, OrdinalSet<InstanceKey> pts) {
		assert (baseField.isStatic());

		Set<FormOutHeapNode> fOuts = findSet(formOutStatic, baseField);
		FormOutHeapNode fOut = findNode(fOuts, id);
		if (fOut == null) {
			fOut = new FormOutHeapNode(baseField, id, isPrimitive, type, pKey, pts);
			fOuts.add(fOut);
		}

		return fOut;
	}


}
