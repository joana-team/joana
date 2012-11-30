/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.pointsto;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.demandpa.alg.IDemandPointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * Wrapper for the points-to analysis. So demand driven points-to analysis may also be used.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class PointsToWrapper implements IPointerAnalysis {

	private final IDemandPointerAnalysis demandPts;
	private final PointerAnalysis pta;

	public PointsToWrapper(IDemandPointerAnalysis demandPts, PointerAnalysis pta) {
		this.demandPts = demandPts;
		this.pta = pta;
	}


	public HeapGraph getHeapGraph() {
		return pta.getHeapGraph();
	}

	public HeapModel getHeapModel() {
		if (demandPts != null) {
			return demandPts.getHeapModel();
		}

		return pta.getHeapModel();
	}

	public OrdinalSet<InstanceKey> getPointsToSet(PointerKey key) {
		Collection<InstanceKey> result = null;

		if (demandPts != null) {
			result = demandPts.getPointsTo(key);
		}

		if (result == null) {
			return pta.getPointsToSet(key);
		} else {
			BitVectorIntSet bvInt = new BitVectorIntSet();

			for (InstanceKey ik : result) {
				int index = pta.getInstanceKeyMapping().getMappedIndex(ik);

				assert (index >= 0);

				bvInt.add(index);
			}

			OrdinalSet<InstanceKey> set = new OrdinalSet<InstanceKey>(bvInt, pta.getInstanceKeyMapping());

			return set;
		}
	}

	public OrdinalSet<InstanceKey> getPointsToSet(Set<PointerKey> keys) {
		OrdinalSet<InstanceKey> result = OrdinalSet.empty();

		for (PointerKey pk : keys) {
			OrdinalSet<InstanceKey> pts = getPointsToSet(pk);
			result = OrdinalSet.unify(result, pts);
		}

		return result;
	}

	private final Map<IClass, OrdinalSet<InstanceKey>> cls2pts = HashMapFactory.make();

	/* (non-Javadoc)
	 * @see edu.kit.joana.deprecated.jsdg.sdg.pointsto.IPointerAnalysis#getArtificialClassFieldPts(com.ibm.wala.classLoader.IClass)
	 */
	@Override
	public OrdinalSet<InstanceKey> getArtificialClassFieldPts(IClass cls) {
		OrdinalSet<InstanceKey> pts = cls2pts.get(cls);
		if (pts == null) {
			HeapModel hm = (demandPts == null ? pta.getHeapModel() : demandPts.getHeapModel());
			InstanceKey ik = hm.getInstanceKeyForClassObject(cls.getReference());

			MutableMapping<InstanceKey> mutable = (MutableMapping<InstanceKey>) pta.getInstanceKeyMapping();
			if (mutable.hasMappedIndex(ik)) {
				throw new IllegalStateException();
			}

			int index = mutable.add(ik);
			pts = mutable.makeSingleton(index);

			cls2pts.put(cls, pts);
		}

		return pts;
	}

}
