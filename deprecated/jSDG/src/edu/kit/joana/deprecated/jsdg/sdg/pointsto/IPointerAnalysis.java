/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.pointsto;

import java.util.Set;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * Basic interface to encapsulate various pointer analyses
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public interface IPointerAnalysis {

	  /**
	   * @param key representative of an equivalence class of pointers
	   * @return Set of InstanceKey, representing the instance abstractions that define
	   * the points-to set computed for the pointer key
	   */
	  OrdinalSet<InstanceKey> getPointsToSet(PointerKey key);

	  OrdinalSet<InstanceKey> getPointsToSet(Set<PointerKey> keys);

	  /**
	   * @return a graph view of the pointer analysis solution
	   */
	  HeapGraph<InstanceKey> getHeapGraph();

	  /**
	   * @return an Object that determines how to model abstract locations in the heap.
	   */
	  HeapModel getHeapModel();

	/**
	 * Returns a points-to set for the .class field of the given class. This is needed
	 * for the interference computation of synchronized static methods.
	 * cls equal cls' => pts == pts'
	 * @param cls Class
	 * @return Points-to set
	 */
	OrdinalSet<InstanceKey> getArtificialClassFieldPts(IClass cls);

}
