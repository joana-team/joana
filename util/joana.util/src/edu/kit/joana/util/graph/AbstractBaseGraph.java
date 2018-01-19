/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Creator:  Barak Naveh (http://sourceforge.net/users/barak_naveh)
 *
 * (C) Copyright 2003-2008, by Barak Naveh and Contributors.
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
/* ----------------------
 * AbstractBaseGraph.java
 * ----------------------
 * (C) Copyright 2003-2008, by Barak Naveh and Contributors.
 *
 * Original Author:  Barak Naveh
 * Contributor(s):   John V. Sichi
 *                   Christian Hammer
 *
 * $Id$
 *
 * Changes
 * -------
 * 24-Jul-2003 : Initial revision (BN);
 * 10-Aug-2003 : General edge refactoring (BN);
 * 06-Nov-2003 : Change edge sharing semantics (JVS);
 * 07-Feb-2004 : Enabled serialization (BN);
 * 11-Mar-2004 : Made generic (CH);
 * 01-Jun-2005 : Added EdgeListFactory (JVS);
 * 07-May-2006 : Changed from List<Edge> to Set<Edge> (JVS);
 * 28-May-2006 : Moved connectivity info from edge to graph (JVS);
 *
 */
package edu.kit.joana.util.graph;

import java.io.*;

import java.util.*;

import org.jgrapht.*;
import org.jgrapht.graph.AbstractGraph;
import org.jgrapht.graph.EdgeSetFactory;
import org.jgrapht.util.*;



/**
 * The most general implementation of the {@link org.jgrapht.Graph} interface.
 * Its subclasses add various restrictions to get more specific graphs. The
 * decision whether it is directed or undirected is decided at construction time
 * and cannot be later modified (see constructor for details).
 *
 * <p>This graph implementation guarantees deterministic vertex and edge set
 * ordering (via {@link LinkedHashMap} and {@link LinkedHashSet}).</p>
 *
 * @author Barak Naveh
 * @since Jul 24, 2003
 */
public abstract class AbstractBaseGraph<V, E extends KnowsVertices<V>>
    extends AbstractGraph<V, E>
    implements Graph<V, E>,
        Cloneable,
        Serializable
{
    private static final long serialVersionUID = -1263088497616142427L;

    private static final String LOOPS_NOT_ALLOWED = "loops not allowed";

    private final boolean allowingLoops;

    private EdgeFactory<V, E> edgeFactory;
    private EdgeSetFactory<V, E> edgeSetFactory;
    private Set<E> edgeSet;
    private transient Set<E> unmodifiableEdgeSet = null;
    private transient Set<V> unmodifiableVertexSet = null;
    private DirectedSpecifics specifics;
    private final boolean allowingMultipleEdges;

    /**
     * Construct a new graph. The graph can either be directed or undirected,
     * depending on the specified edge factory.
     *
     * @param ef the edge factory of the new graph.
     * @param allowMultipleEdges whether to allow multiple edges or not.
     * @param allowLoops whether to allow edges that are self-loops or not.
     *
     * @throws NullPointerException if the specified edge factory is <code>
     * null</code>.
     */
    protected AbstractBaseGraph(
        EdgeFactory<V, E> ef,
        boolean allowMultipleEdges,
        boolean allowLoops)
    {
        if (ef == null) {
            throw new NullPointerException();
        }

        edgeSet = new HashSet<>();
        edgeFactory = ef;
        allowingLoops = allowLoops;
        allowingMultipleEdges = allowMultipleEdges;

        specifics = new DirectedSpecifics();

        this.edgeSetFactory = new ArrayListFactory<V, E>();
    }

    /**
     * @see Graph#getAllEdges(Object, Object)
     */
    @Override public Set<E> getAllEdges(V sourceVertex, V targetVertex)
    {
        return specifics.getAllEdges(sourceVertex, targetVertex);
    }

    /**
     * Returns <code>true</code> if and only if self-loops are allowed in this
     * graph. A self loop is an edge that its source and target vertices are the
     * same.
     *
     * @return <code>true</code> if and only if graph loops are allowed.
     */
    public boolean isAllowingLoops()
    {
        return allowingLoops;
    }

    /**
     * Returns <code>true</code> if and only if multiple edges are allowed in
     * this graph. The meaning of multiple edges is that there can be many edges
     * going from vertex v1 to vertex v2.
     *
     * @return <code>true</code> if and only if multiple edges are allowed.
     */
    public boolean isAllowingMultipleEdges()
    {
        return allowingMultipleEdges;
    }

    /**
     * @see Graph#getEdge(Object, Object)
     */
    @Override public E getEdge(V sourceVertex, V targetVertex)
    {
        return specifics.getEdge(sourceVertex, targetVertex);
    }

    /**
     * @see Graph#getEdgeFactory()
     */
    @Override public EdgeFactory<V, E> getEdgeFactory()
    {
        return edgeFactory;
    }

    /**
     * Set the {@link EdgeSetFactory} to use for this graph. Initially, a graph
     * is created with a default implementation which always supplies an {@link
     * java.util.ArrayList} with capacity 1.
     *
     * @param edgeSetFactory factory to use for subsequently created edge sets
     * (this call has no effect on existing edge sets)
     */
    public void setEdgeSetFactory(EdgeSetFactory<V, E> edgeSetFactory)
    {
        this.edgeSetFactory = edgeSetFactory;
    }

    /**
     * @see Graph#addEdge(Object, Object)
     */
    @Override public E addEdge(V sourceVertex, V targetVertex)
    {
        assertVertexExist(sourceVertex);
        assertVertexExist(targetVertex);

        if (!allowingMultipleEdges
            && containsEdge(sourceVertex, targetVertex))
        {
            return null;
        }

        if (!allowingLoops && sourceVertex.equals(targetVertex)) {
            throw new IllegalArgumentException(LOOPS_NOT_ALLOWED);
        }

        E e = edgeFactory.createEdge(sourceVertex, targetVertex);

        if (containsEdge(e)) { // this restriction should stay!

            return null;
        } else {
            edgeSet.add(e);
            specifics.addEdgeToTouchingVertices(e);

            return e;
        }
    }

    /**
     * @see Graph#addEdge(Object, Object, Object)
     */
    @Override public boolean addEdge(V sourceVertex, V targetVertex, E e)
    {
        if (e == null) {
            throw new NullPointerException();
        } else if (containsEdge(e)) {
            return false;
        }

        assertVertexExist(sourceVertex);
        assertVertexExist(targetVertex);

        if (!allowingMultipleEdges
            && containsEdge(sourceVertex, targetVertex))
        {
            return false;
        }

        if (!allowingLoops && sourceVertex.equals(targetVertex)) {
            throw new IllegalArgumentException(LOOPS_NOT_ALLOWED);
        }

        edgeSet.add(e);
        specifics.addEdgeToTouchingVertices(e);

        return true;
    }
    
    /**
     * @see Graph#addEdge(Object, Object, Object),
     * except some failure cases may go unnoticed unless running with java assertions enabled
     */
    public boolean addEdgeUnsafe(V sourceVertex, V targetVertex, E e)
    {
        if (e == null) {
            throw new NullPointerException();
        }

        assert assertVertexExist(sourceVertex);
        assert assertVertexExist(targetVertex);

        if (!allowingMultipleEdges
            && containsEdge(sourceVertex, targetVertex))
        {
            return false;
        }

        if (!allowingLoops && sourceVertex.equals(targetVertex)) {
            throw new IllegalArgumentException(LOOPS_NOT_ALLOWED);
        }

        final boolean added = edgeSet.add(e);
        
        if (added) {
            specifics.addEdgeToTouchingVerticesUnsafe(e);
        }

        return added;
    }

    /**
     * @see Graph#addVertex(Object)
     */
    @Override public boolean addVertex(V v)
    {
        if (v == null) {
            throw new NullPointerException();
        } else if (containsVertex(v)) {
            return false;
        } else {
            specifics.addVertex(v);

            return true;
        }
    }

    /**
     * @see Graph#getEdgeSource(Object)
     */
    @Override public V getEdgeSource(E e)
    {
        return e.getSource();
    }

    /**
     * @see Graph#getEdgeTarget(Object)
     */
    @Override public V getEdgeTarget(E e)
    {
        return e.getTarget();
    }

    /**
     * Returns a shallow copy of this graph instance. Neither edges nor vertices
     * are cloned.
     *
     * @return a shallow copy of this set.
     *
     * @throws RuntimeException
     *
     * @see java.lang.Object#clone()
     */
    @Override public Object clone()
    {
        try {
            TypeUtil<AbstractBaseGraph<V, E>> typeDecl = null;

            AbstractBaseGraph<V, E> newGraph =
                TypeUtil.uncheckedCast(super.clone(), typeDecl);

            newGraph.edgeSet = new HashSet<>();

            newGraph.edgeFactory = this.edgeFactory;
            newGraph.unmodifiableEdgeSet = null;
            newGraph.unmodifiableVertexSet = null;

            // NOTE:  it's important for this to happen in an object
            // method so that the new inner class instance gets associated with
            // the right outer class instance
            newGraph.specifics = new DirectedSpecifics();

            org.jgrapht.Graphs.addGraph(newGraph, this);

            return newGraph;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * @see Graph#containsEdge(Object)
     */
    @Override public boolean containsEdge(E e)
    {
        return edgeSet.contains(e);
    }

    /**
     * @see Graph#containsVertex(Object)
     */
    @Override public boolean containsVertex(V v)
    {
        return specifics.getVertexSet().contains(v);
    }

    /**
     * @see UndirectedGraph#degreeOf(Object)
     */
    public int degreeOf(V vertex)
    {
        return specifics.degreeOf(vertex);
    }

    /**
     * @see Graph#edgeSet()
     */
    @Override public Set<E> edgeSet()
    {
        if (unmodifiableEdgeSet == null) {
            unmodifiableEdgeSet = new EdgeSetView(specifics.vertexMapDirected);
        }

        return unmodifiableEdgeSet;
    }
    
    private class EdgeSetView implements Set<E> {
    	final Map<V, DirectedEdgeContainer<V, E>> vertexMap;
    	EdgeSetView(Map<V, DirectedEdgeContainer<V, E>> vertexMap) {
    		this.vertexMap = vertexMap;
    	}

		/* (non-Javadoc)
		 * @see java.util.Set#add(java.lang.Object)
		 */
		@Override
		public boolean add(E e) {
			throw new UnsupportedOperationException();
		}

		/* (non-Javadoc)
		 * @see java.util.Set#addAll(java.util.Collection)
		 */
		@Override
		public boolean addAll(Collection<? extends E> c) {
			throw new UnsupportedOperationException();
		}

		/* (non-Javadoc)
		 * @see java.util.Set#clear()
		 */
		@Override
		public void clear() {
			throw new UnsupportedOperationException();
			
		}

		/* (non-Javadoc)
		 * @see java.util.Set#contains(java.lang.Object)
		 */
		@Override
		public boolean contains(Object o) {
			if (!(o instanceof KnowsVertices)) {
				return false;
			}
			DirectedEdgeContainer<V, E> vc
					= vertexMap.get(((KnowsVertices<?>) o).getSource());
			if (vc == null || vc.outgoing == null) {
				return false;
			}
			return vc.outgoing.contains(o);
		}

		/* (non-Javadoc)
		 * @see java.util.Set#containsAll(java.util.Collection)
		 */
		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object o : c) {
				if (!contains(o)) {
					return false;
				}
			}
			return true;
		}

		/* (non-Javadoc)
		 * @see java.util.Set#isEmpty()
		 */
		@Override
		public boolean isEmpty() {
			return size() == 0;
		}

		/* (non-Javadoc)
		 * @see java.util.Set#iterator()
		 */
		@Override
		public Iterator<E> iterator() {
			return new EdgeSetViewIterator() ;
		}

		/* (non-Javadoc)
		 * @see java.util.Set#remove(java.lang.Object)
		 */
		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		/* (non-Javadoc)
		 * @see java.util.Set#removeAll(java.util.Collection)
		 */
		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		/* (non-Javadoc)
		 * @see java.util.Set#retainAll(java.util.Collection)
		 */
		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		/* (non-Javadoc)
		 * @see java.util.Set#size()
		 */
		@Override
		public int size() {
			// since the "outgoing" is initialized lazily, c may be null for a vertex w/o outgoing edges/  
			return vertexMap.values().stream().mapToInt(c-> (c == null) ? 0 : c.outgoing.size()).sum();
		}

		/* (non-Javadoc)
		 * @see java.util.Set#toArray()
		 */
		@Override
		public Object[] toArray() {
			return toList().toArray();
		}

		/* (non-Javadoc)
		 * @see java.util.Set#toArray(java.lang.Object[])
		 */
		@Override
		public <T> T[] toArray(T[] a) {
			return toList().toArray(a);
		}
		
		private List<E> toList() {
			List<E> tmp = new ArrayList<>();
			for (E e : this) {
				tmp.add(e);
			}
			return tmp;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			String comma = "";
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			for (E e : this) {
				sb.append(comma);
				sb.append(e);
				comma = ",";
			}
			sb.append("]");
			return sb.toString();
		}
    	
		private class EdgeSetViewIterator implements Iterator<E>{
			Iterator<DirectedEdgeContainer<V, E>> ecIt;
			Iterator<E> edgeIt;
			E next;
			
			EdgeSetViewIterator() {
				ecIt = vertexMap.values().iterator();
				advance();
			}

			@Override
			public boolean hasNext() {
				return next != null;
			}
	
			@Override
			public E next() {
				E ret = next;
				if (edgeIt.hasNext()) {
					next = edgeIt.next();
				} else {
					advance();
				}
				return ret;
			}
			
			private void advance() {
				DirectedEdgeContainer<V, E> vc = null;
				while (ecIt.hasNext()) {
					vc = ecIt.next();
					if (vc != null && !vc.outgoing.isEmpty()) {
						edgeIt = vc.outgoing.iterator();
						next = edgeIt.next();
						return;
					}
				}
				next = null;
			}
		}
    }

    /**
     * @see Graph#edgesOf(Object)
     */
    @Override public Set<E> edgesOf(V vertex)
    {
        return specifics.edgesOf(vertex);
    }

    /**
     * @see DirectedGraph#inDegreeOf(Object)
     */
    public int inDegreeOf(V vertex)
    {
        return specifics.inDegreeOf(vertex);
    }

    /**
     * @see DirectedGraph#incomingEdgesOf(Object)
     */
    public Set<E> incomingEdgesOf(V vertex)
    {
        return specifics.incomingEdgesOf(vertex);
    }

    /**
     * @see DirectedGraph#outDegreeOf(Object)
     */
    public int outDegreeOf(V vertex)
    {
        return specifics.outDegreeOf(vertex);
    }

    /**
     * @see DirectedGraph#outgoingEdgesOf(Object)
     */
    public Set<E> outgoingEdgesOf(V vertex)
    {
        return specifics.outgoingEdgesOf(vertex);
    }

    /**
     * @see Graph#removeEdge(Object, Object)
     */
    @Override public E removeEdge(V sourceVertex, V targetVertex)
    {
        E e = getEdge(sourceVertex, targetVertex);

        if (e != null) {
            specifics.removeEdgeFromTouchingVertices(e);
            edgeSet.remove(e);
        }

        return e;
    }

    /**
     * @see Graph#removeEdge(Object)
     */
    @Override public boolean removeEdge(E e)
    {
        if (containsEdge(e)) {
            specifics.removeEdgeFromTouchingVertices(e);
            edgeSet.remove(e);

            return true;
        } else {
            return false;
        }
    }

    /**
     * @see Graph#removeVertex(Object)
     */
    @Override public boolean removeVertex(V v)
    {
        if (containsVertex(v)) {
            Set<E> touchingEdgesList = edgesOf(v);

            // cannot iterate over list - will cause
            // ConcurrentModificationException
            removeAllEdges(new ArrayList<E>(touchingEdgesList));

            specifics.getVertexSet().remove(v); // remove the vertex itself

            return true;
        } else {
            return false;
        }
    }

    /**
     * @see Graph#vertexSet()
     */
    @Override public Set<V> vertexSet()
    {
        if (unmodifiableVertexSet == null) {
            unmodifiableVertexSet =
                Collections.unmodifiableSet(specifics.getVertexSet());
        }

        return unmodifiableVertexSet;
    }

    /**
     * @see Graph#getEdgeWeight(Object)
     */
    @Override public double getEdgeWeight(E e)
    {
    	if (e == null) {
            throw new NullPointerException();
        } else {
            return WeightedGraph.DEFAULT_EDGE_WEIGHT;
        }
    }

    private static class ArrayListFactory<VV, EE>
        implements EdgeSetFactory<VV, EE>,
            Serializable
    {
        private static final long serialVersionUID = 5936902837403445985L;

        /**
         * @see EdgeSetFactory.createEdgeSet
         */
        @Override public Set<EE> createEdgeSet(VV vertex)
        {
            // NOTE:  use size 1 to keep memory usage under control
            // for the common case of vertices with low degree
            return new ArrayUnenforcedSet<EE>(1);
        }
    }

    /**
     * A container for vertex edges.
     *
     * <p>In this edge container we use array lists to minimize memory toll.
     * However, for high-degree vertices we replace the entire edge container
     * with a direct access subclass (to be implemented).</p>
     *
     * @author Barak Naveh
     */
    protected static class DirectedEdgeContainer<VV, EE>
        implements Serializable
    {
        private static final long serialVersionUID = 7494242245729767106L;
        Set<EE> incoming;
        Set<EE> outgoing;
        private transient Set<EE> unmodifiableIncoming = null;
        private transient Set<EE> unmodifiableOutgoing = null;

        DirectedEdgeContainer(EdgeSetFactory<VV, EE> edgeSetFactory,
            VV vertex)
        {
            incoming = edgeSetFactory.createEdgeSet(vertex);
            outgoing = edgeSetFactory.createEdgeSet(vertex);
        }

        /**
         * A lazy build of unmodifiable incoming edge set.
         *
         * @return
         */
        public Set<EE> getUnmodifiableIncomingEdges()
        {
            if (unmodifiableIncoming == null) {
                unmodifiableIncoming = Collections.unmodifiableSet(incoming);
            }

            return unmodifiableIncoming;
        }

        /**
         * A lazy build of unmodifiable outgoing edge set.
         *
         * @return
         */
        public Set<EE> getUnmodifiableOutgoingEdges()
        {
            if (unmodifiableOutgoing == null) {
                unmodifiableOutgoing = Collections.unmodifiableSet(outgoing);
            }

            return unmodifiableOutgoing;
        }

        /**
         * .
         *
         * @param e
         */
        public void addIncomingEdge(EE e)
        {
            incoming.add(e);
        }

        /**
         * .
         *
         * @param e
         */
        public void addOutgoingEdge(EE e)
        {
            outgoing.add(e);
        }

        /**
         * .
         *
         * @param e
         */
        public void removeIncomingEdge(EE e)
        {
            incoming.remove(e);
        }

        /**
         * .
         *
         * @param e
         */
        public void removeOutgoingEdge(EE e)
        {
            outgoing.remove(e);
        }
    }

    /**
     * .
     *
     * @author Barak Naveh
     */
    protected class DirectedSpecifics
        implements Serializable
    {
        private static final long serialVersionUID = 8971725103718958232L;
        private static final String NOT_IN_DIRECTED_GRAPH =
            "no such operation in a directed graph";

        protected Map<V, DirectedEdgeContainer<V, E>> vertexMapDirected;

        public DirectedSpecifics()
        {
            this(new LinkedHashMap<V, DirectedEdgeContainer<V, E>>());
        }

        public DirectedSpecifics(Map<V, DirectedEdgeContainer<V, E>> vertexMap)
        {
            this.vertexMapDirected = vertexMap;
        }

        public void addVertex(V v)
        {
            // add with a lazy edge container entry
            vertexMapDirected.put(v, null);
        }

        public Set<V> getVertexSet()
        {
            return vertexMapDirected.keySet();
        }

        /**
         * @see Graph#getAllEdges(Object, Object)
         */
        public Set<E> getAllEdges(V sourceVertex, V targetVertex)
        {
            Set<E> edges = null;

            if (containsVertex(sourceVertex)
                && containsVertex(targetVertex))
            {
                edges = new ArrayUnenforcedSet<E>();

                DirectedEdgeContainer<V, E> ec = getEdgeContainer(sourceVertex);

                Iterator<E> iter = ec.outgoing.iterator();

                while (iter.hasNext()) {
                    E e = iter.next();

                    if (getEdgeTarget(e).equals(targetVertex)) {
                        edges.add(e);
                    }
                }
            }

            return edges;
        }

        /**
         * @see Graph#getEdge(Object, Object)
         */
        public E getEdge(V sourceVertex, V targetVertex)
        {
            if (containsVertex(sourceVertex)
                && containsVertex(targetVertex))
            {
                DirectedEdgeContainer<V, E> ec = getEdgeContainer(sourceVertex);

                Iterator<E> iter = ec.outgoing.iterator();

                while (iter.hasNext()) {
                    E e = iter.next();

                    if (getEdgeTarget(e).equals(targetVertex)) {
                        return e;
                    }
                }
            }

            return null;
        }

        public void addEdgeToTouchingVertices(E e)
        {
            V source = getEdgeSource(e);
            V target = getEdgeTarget(e);

            getEdgeContainer(source).addOutgoingEdge(e);
            getEdgeContainer(target).addIncomingEdge(e);
        }
        
        public void addEdgeToTouchingVerticesUnsafe(E e)
        {
            V source = getEdgeSource(e);
            V target = getEdgeTarget(e);

            getEdgeContainerUnsafe(source).addOutgoingEdge(e);
            getEdgeContainerUnsafe(target).addIncomingEdge(e);
        }

        /**
         * @see UndirectedGraph#degreeOf(Object)
         */
        public int degreeOf(V vertex)
        {
            throw new UnsupportedOperationException(NOT_IN_DIRECTED_GRAPH);
        }

        /**
         * @see Graph#edgesOf(Object)
         */
        public Set<E> edgesOf(V vertex)
        {
            ArrayUnenforcedSet<E> inAndOut =
                new ArrayUnenforcedSet<E>(getEdgeContainer(vertex).incoming);
            inAndOut.addAll(getEdgeContainer(vertex).outgoing);

            // we have two copies for each self-loop - remove one of them.
            if (allowingLoops) {
                Set<E> loops = getAllEdges(vertex, vertex);

                for (int i = 0; i < inAndOut.size();) {
                    Object e = inAndOut.get(i);

                    if (loops.contains(e)) {
                        inAndOut.remove(i);
                        loops.remove(e); // so we remove it only once
                    } else {
                        i++;
                    }
                }
            }

            return Collections.unmodifiableSet(inAndOut);
        }

        /**
         * @see DirectedGraph#inDegreeOf(Object)
         */
        public int inDegreeOf(V vertex)
        {
            return getEdgeContainer(vertex).incoming.size();
        }

        /**
         * @see DirectedGraph#incomingEdgesOf(Object)
         */
        public Set<E> incomingEdgesOf(V vertex)
        {
            return getEdgeContainer(vertex).getUnmodifiableIncomingEdges();
        }

        /**
         * @see DirectedGraph#outDegreeOf(Object)
         */
        public int outDegreeOf(V vertex)
        {
            return getEdgeContainer(vertex).outgoing.size();
        }

        /**
         * @see DirectedGraph#outgoingEdgesOf(Object)
         */
        public Set<E> outgoingEdgesOf(V vertex)
        {
            return getEdgeContainer(vertex).getUnmodifiableOutgoingEdges();
        }

        public void removeEdgeFromTouchingVertices(E e)
        {
            V source = getEdgeSource(e);
            V target = getEdgeTarget(e);

            getEdgeContainer(source).removeOutgoingEdge(e);
            getEdgeContainer(target).removeIncomingEdge(e);
        }

        /**
         * A lazy build of edge container for specified vertex.
         *
         * @param vertex a vertex in this graph.
         *
         * @return EdgeContainer
         */
        private DirectedEdgeContainer<V, E> getEdgeContainer(V vertex)
        {
            assertVertexExist(vertex);

            return vertexMapDirected.compute(vertex, (v, ec) -> {

            if (ec == null) {
                ec = new DirectedEdgeContainer<V, E>(edgeSetFactory, vertex);
            }

            return ec;
            });
        }
        
        /**
         * A lazy build of edge container for specified vertex.
         *
         * @param vertex a vertex in this graph.
         *
         * @return EdgeContainer
         */
        private DirectedEdgeContainer<V, E> getEdgeContainerUnsafe(V vertex)
        {
            assert assertVertexExist(vertex);

            return vertexMapDirected.compute(vertex, (v, ec) -> {

            if (ec == null) {
                ec = new DirectedEdgeContainer<V, E>(edgeSetFactory, vertex);
            }

            return ec;
            });
        }
    }

}

// End AbstractBaseGraph.java
