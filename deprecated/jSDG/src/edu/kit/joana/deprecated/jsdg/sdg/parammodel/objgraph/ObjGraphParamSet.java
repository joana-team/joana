/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph;

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.util.collections.HashSetFactory;

import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamSet;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParameter;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ObjGraphParameter;

/**
 * Set of parameter nodes for the object graph model.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ObjGraphParamSet implements IParamSet<ObjGraphParameter> {

	private final Set<ObjGraphParameter> nodes;

	public static final ObjGraphParamSet emptySet = new ObjGraphParamSet() {
		public void add(ObjGraphParameter node) {
			throw new UnsupportedOperationException();
		}

		public void merge(IParamSet<ObjGraphParameter> set) {
			throw new UnsupportedOperationException();
		}
	};

	public ObjGraphParamSet() {
		this.nodes = HashSetFactory.make();
	}

	public boolean isEmpty() {
		return nodes.isEmpty();
	}

	public void add(ObjGraphParameter node) {
		nodes.add(node);
	}

	public void remove(ObjGraphParameter node) {
		nodes.remove(node);
	}

	public boolean contains(ObjGraphParameter node) {
		return nodes.contains(node);
	}

	public boolean containsAny(ObjGraphParamSet set) {
		for (ObjGraphParameter wp : set) {
			if (nodes.contains(wp)) {
				return true;
			}
		}

		return false;
	}

	private boolean isEqual(ObjGraphParamSet set) {
		return nodes.equals(set.nodes);
	}

	private void mergeIt(ObjGraphParamSet set) {
		nodes.addAll(set.nodes);
	}

	public boolean equals(IParamSet<ObjGraphParameter> set) {
		return (set != null) && (set instanceof ObjGraphParamSet) && isEqual((ObjGraphParamSet) set);
	}

	public void merge(IParamSet<ObjGraphParameter> set) {
		if (set == null) {
			return;
		} else if (set instanceof ObjGraphParamSet) {
			mergeIt((ObjGraphParamSet) set);
		} else {
			throw new UnsupportedOperationException("Can not merge set with type "
				+ set.getClass().getCanonicalName());
		}
	}

	public void removeAll(ObjGraphParamSet set) {
		nodes.removeAll(set.nodes);
	}

	public Iterator<ObjGraphParameter> iterator() {
		return nodes.iterator();
	}

	public boolean contains(IParameter param) {
		return nodes.contains(param);
	}

}
