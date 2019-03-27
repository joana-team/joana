/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.graph;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jgrapht.EdgeFactory;

import edu.kit.joana.util.collections.Disowning;


/**
 * This is the base class for all concrete graphs used in the JOANA project.
 * @author Martin Mohr
 */
public abstract class AbstractJoanaGraph<V extends IntegerIdentifiable, E extends KnowsVertices<V>> extends DirectedPseudograph<V, E> {
	
	private static final long serialVersionUID = -2398430494628192648L;

	public AbstractJoanaGraph(EdgeFactory<V, E> edgeFactory, Supplier<Map<V,DirectedEdgeContainer<E, E[]>>> vertexMapConstructor, Class<E> classE) {
		super(edgeFactory, vertexMapConstructor, classE);
	}
	
	public AbstractJoanaGraph(EdgeFactory<V, E> edgeFactory, Supplier<Map<V,DirectedEdgeContainer<E, E[]>>> vertexMapConstructor, Function<E[], Disowning<E>> asProvider, Class<E> classE) {
		super(edgeFactory, vertexMapConstructor, asProvider, classE);
	}

	public AbstractJoanaGraph(Class<E> edgeClass, Supplier<Map<V,DirectedEdgeContainer<E, E[]>>> vertexMapConstructor, Class<E> classE) {
		super(edgeClass, vertexMapConstructor, classE);
	}
}
