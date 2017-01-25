/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree;

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.util.collections.HashSetFactory;

import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamSet;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParameter;

/**
 * Parameter set of an object tree
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ObjTreeParamSet implements IParamSet<ParameterNode<?>> {

	private final Set<ParameterNode<?>> nodes;

	public ObjTreeParamSet() {
		this.nodes = HashSetFactory.make();
	}

	public boolean isEmpty() {
		return nodes.isEmpty();
	}

	public void add(ParameterNode<?> node) {
		nodes.add(node);
	}

	public boolean contains(ParameterNode<?> node) {
		return nodes.contains(node);
	}

	private boolean isEqual(ObjTreeParamSet set) {
		return nodes.equals(set.nodes);
	}

	private void mergeIt(ObjTreeParamSet set) {
		nodes.addAll(set.nodes);
	}

	public boolean equals(IParamSet<ParameterNode<?>> set) {
		return (set != null) && (set instanceof ObjTreeParamSet) && isEqual((ObjTreeParamSet) set);
	}

	public void merge(IParamSet<ParameterNode<?>> set) {
		if (set == null) {
			return;
		} else if (set instanceof ObjTreeParamSet) {
			mergeIt((ObjTreeParamSet) set);
		} else {
			throw new UnsupportedOperationException("Can not merge set with type "
				+ set.getClass().getCanonicalName());
		}
	}

	public Iterator<ParameterNode<?>> iterator() {
		return nodes.iterator();
	}

	public boolean contains(IParameter param) {
		return nodes.contains(param);
	}

}
