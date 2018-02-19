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
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jgrapht.*;
import org.jgrapht.graph.AbstractGraph;
import org.jgrapht.util.*;


import edu.kit.joana.util.collections.ArraySet;
import edu.kit.joana.util.collections.ModifiableArraySet;
import edu.kit.joana.util.collections.SimpleVectorBase;



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
public abstract class AbstractBaseGraph<V extends IntegerIdentifiable, E extends KnowsVertices<V>>
    extends AbstractGraph<V, E>
    implements Graph<V, E>,
        Cloneable,
        Serializable
{
    private static final long serialVersionUID = -1263088497616142427L;

    private static final String LOOPS_NOT_ALLOWED = "loops not allowed";

    private final boolean allowingLoops;

    private EdgeFactory<V, E> edgeFactory;
    private transient Set<E> unmodifiableEdgeSet = null;
    private transient Set<V> unmodifiableVertexSet = null;
    private DirectedSpecifics specifics;
    private final boolean allowingMultipleEdges;
    private final Supplier<Map<V,DirectedEdgeContainer<E,E[]>>> vertexMapConstructor;

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
    protected <T> AbstractBaseGraph(
        EdgeFactory<V, E> ef,
        boolean allowMultipleEdges,
        boolean allowLoops,
        Supplier<Map<V,DirectedEdgeContainer<E,E[]>>> vertexMapConst
        )
    {
        if (ef == null) {
            throw new NullPointerException();
        }

        edgeFactory = ef;
        allowingLoops = allowLoops;
        allowingMultipleEdges = allowMultipleEdges;
        vertexMapConstructor = vertexMapConst;

        Map<V, DirectedEdgeContainer<E,E[]>> vertexMap = vertexMapConstructor.get();
        specifics = new DirectedSpecifics(vertexMap);

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

        if (sourceVertex != null && !(containsVertex(sourceVertex))) {
        	containsVertex(sourceVertex);
        	throw new IllegalStateException();
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

        return specifics.addEdgeToTouchingVerticesUnsafe(e);
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

            newGraph.edgeFactory = this.edgeFactory;
            newGraph.unmodifiableEdgeSet = null;
            newGraph.unmodifiableVertexSet = null;

            // NOTE:  it's important for this to happen in an object
            // method so that the new inner class instance gets associated with
            // the right outer class instance
            newGraph.specifics = new DirectedSpecifics(vertexMapConstructor.get());

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
    	return specifics.containsEdge(e);
    }
    
    public boolean containsEdge(V sourceVertex, V targetVertex, Predicate<E> predicate) {
    	final Set<E> outgoing = specifics.outgoingEdgesOf(sourceVertex);
    	for (E e : outgoing) {
    		if (e.getTarget().equals(targetVertex) && predicate.test(e)) return true;
    	}
    	return false;
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
    	final Map<V, DirectedEdgeContainer<E, E[]>> vertexMap;
    	EdgeSetView(Map<V, DirectedEdgeContainer<E, E[]>> vertexMap) {
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
			DirectedEdgeContainer<E, E[]> vc
					= vertexMap.get(((KnowsVertices<?>) o).getSource());
			
			if (vc == null || vc.outgoing() == null) {
				return false;
			}
			final ArraySet<E> outgoing = ArraySet.own(vc.outgoing());
			return outgoing.contains(o);
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
			return vertexMap.values().stream().mapToInt(c-> (c == null) ? 0 : ArraySet.own(c.outgoing()).size()).sum();
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
			Iterator<DirectedEdgeContainer<E, E[]>> ecIt;
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
				DirectedEdgeContainer<E, E[]> vc = null;
				while (ecIt.hasNext()) {
					vc = ecIt.next();
					Set<E> outgoing = ArraySet.own(vc.outgoing());
					if (vc != null && !outgoing.isEmpty()) {
						edgeIt = outgoing.iterator();
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
     * @see DirectedGraph#incomingEdgesOf(Object)
     */
    public Set<E> incomingEdgesOfUnsafe(V vertex)
    {
        return specifics.incomingEdgesOfUnsafe(vertex);
    }
    
    public void removeIncomingEdgesOf(V vertex) {
    	specifics.removeIncomingEdgesOf(vertex);
    }
    
    public void removeOutgoingEdgesOf(V vertex) {
    	specifics.removeOutgoingEdgesOf(vertex);
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
     * @see DirectedGraph#outgoingEdgesOf(Object)
     */
    public Set<E> outgoingEdgesOfUnsafe(V vertex)
    {
        return specifics.outgoingEdgesOfUnsafe(vertex);
    }

    /**
     * @see Graph#removeEdge(Object, Object)
     */
    @Override public E removeEdge(V sourceVertex, V targetVertex)
    {
        E e = getEdge(sourceVertex, targetVertex);

        if (e != null) {
            specifics.removeEdgeFromTouchingVertices(e);
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
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * @see Graph#removeEdge(Object)
     */
    public boolean removeEdgeUnsafe(E e)
    {
        return specifics.removeEdgeFromTouchingVertices(e);
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
    
    public void trimToSize() {
    	specifics.trimToSize();
    	
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

    
    public interface DirectedEdgeContainer<EE, Rep> {
        Set<EE> getUnmodifiableIncomingEdges();
        Set<EE> getUnmodifiableOutgoingEdges();
        boolean addIncomingEdge(EE e);
        boolean addOutgoingEdge(EE e);
        boolean removeIncomingEdge(EE e);
        boolean removeOutgoingEdge(EE e);
        void removeIncomingEdges();
        void removeOutgoingEdges();
        Rep incoming();
        Rep outgoing();
        
    }
    /**
     * A container for vertex edges.
     *
     * <p>In this edge container we use arrays to minimize memory toll.
     *
     * @author Martin Hecker
     */
    protected static final class ArraySetDirectedEdgeContainer<EE> implements DirectedEdgeContainer<EE, EE[]>
    {
        EE[] incoming;
        EE[] outgoing;

        ArraySetDirectedEdgeContainer()
        {
        }
        
        @Override
        public EE[] incoming() {
        	return incoming;
        }
        
        @Override
        public EE[] outgoing() {
        	return outgoing;
        }
        

        public final Set<EE> getUnmodifiableIncomingEdges()
        {
        	if (incoming == null) return Collections.emptySet();
            return ArraySet.own(incoming);
        }

        public final Set<EE> getUnmodifiableOutgoingEdges()
        {
        	if (outgoing == null) return Collections.emptySet();
            return ArraySet.own(outgoing);
        }

        /**
         * .
         *
         * @param e
         */
        public final boolean addIncomingEdge(EE e)
        {
        	final ModifiableArraySet<EE> set;
        	final boolean added;
            if (incoming == null) {
            	set = new ModifiableArraySet<>(Collections.singleton(e));
            	added = true;
            } else {
            	set = ModifiableArraySet.own(incoming);
            	added = set.add(e);
            }
            incoming = set.disown();
            return added;
        }

        /**
         * .
         *
         * @param e
         */
        public final boolean addOutgoingEdge(EE e)
        {
        	final ModifiableArraySet<EE> set;
        	final boolean added;
            if (outgoing == null) {
            	set = new ModifiableArraySet<>(Collections.singleton(e));
            	added = true;
            } else {
            	set = ModifiableArraySet.own(outgoing);
            	added = set.add(e);
            }
            outgoing = set.disown();
            return added;
        }

        /**
         * .
         *
         * @param e
         */
        public final boolean removeIncomingEdge(EE e)
        {
        	if (incoming == null) return false;
        	final ModifiableArraySet<EE> set = ModifiableArraySet.own(incoming);
        	final boolean removed = set.remove(e);
            incoming = set.disown();
            return removed;
        }

        /**
         * .
         *
         * @param e
         */
        public final boolean removeOutgoingEdge(EE e)
        {
        	if (outgoing == null) return false;
        	final ModifiableArraySet<EE> set = ModifiableArraySet.own(outgoing);
        	final boolean removed = set.remove(e);
        	outgoing = set.disown();
        	return removed;
        }
        
        @Override
        public void removeIncomingEdges() {
        	incoming = null;
        }
        
        @Override
        public void removeOutgoingEdges() {
        	outgoing = null;
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

        protected Map<V, DirectedEdgeContainer<E,E[]>> vertexMapDirected;
        
        public DirectedSpecifics(Map<V, DirectedEdgeContainer<E,E[]>> vertexMap)
        {
            this.vertexMapDirected = vertexMap;
        }

        public void addVertex(V v)
        {
            vertexMapDirected.put(v, new ArraySetDirectedEdgeContainer<E>());
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
            DirectedEdgeContainer<E,E[]> ec = getEdgeContainer(sourceVertex);
            if (ec == null) return null;
            
            ArraySet<E> outgoing = ArraySet.own(ec.outgoing());
            final Set<E> edges = new ArrayUnenforcedSet<E>(outgoing.size());
            
            for (E e : outgoing) {
            	if (getEdgeTarget(e).equals(targetVertex)) {
            		edges.add(e);
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
            	DirectedEdgeContainer<E, E[]> ec = getEdgeContainer(sourceVertex);

                ArraySet<E> outgoing = ArraySet.own(ec.outgoing());
                Iterator<E> iter = outgoing.iterator();

                while (iter.hasNext()) {
                    E e = iter.next();

                    if (getEdgeTarget(e).equals(targetVertex)) {
                        return e;
                    }
                }
            }

            return null;
        }
        
        public boolean containsEdge(E edge) {
            final Set<E> outgoing = ArraySet.own(getEdgeContainerUnsafe(edge.getSource()).outgoing());
            return outgoing.contains(edge);
        }

        public void addEdgeToTouchingVertices(E e)
        {
            V source = getEdgeSource(e);
            V target = getEdgeTarget(e);

            getEdgeContainer(source).addOutgoingEdge(e);
            getEdgeContainer(target).addIncomingEdge(e);
        }
        
        public boolean addEdgeToTouchingVerticesUnsafe(E e)
        {
            V source = e.getSource();
            V target = e.getTarget();

            final boolean addedInSource = getEdgeContainerUnsafe(source).addOutgoingEdge(e);
            final boolean addedInTarget = getEdgeContainerUnsafe(target).addIncomingEdge(e);
            assert addedInSource == addedInTarget;
            
            return addedInSource;
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
        	Set<E> incoming = ArraySet.own(getEdgeContainer(vertex).incoming());
        	Set<E> outgoing = ArraySet.own(getEdgeContainer(vertex).outgoing());
            Set<E> inAndOut = 
                new HashSet<E>(incoming.size() + outgoing.size());
            inAndOut.addAll(incoming);
            inAndOut.addAll(outgoing);

            return Collections.unmodifiableSet(inAndOut);
        }

        /**
         * @see DirectedGraph#inDegreeOf(Object)
         */
        public int inDegreeOf(V vertex)
        {
        	ArraySet<E> incoming = ArraySet.own(getEdgeContainer(vertex).incoming());
            return incoming.size();
        }

        /**
         * @see DirectedGraph#incomingEdgesOf(Object)
         */
        public Set<E> incomingEdgesOf(V vertex)
        {
            return getEdgeContainer(vertex).getUnmodifiableIncomingEdges();
        }
        
        /**
         * @see DirectedGraph#incomingEdgesOf(Object)
         */
        public Set<E> incomingEdgesOfUnsafe(V vertex)
        {
            return getEdgeContainerUnsafe(vertex).getUnmodifiableIncomingEdges();
        }

        public void removeIncomingEdgesOf(V vertex)
        {
        	final Set<E> incoming = getEdgeContainer(vertex).getUnmodifiableIncomingEdges();
        	for (E e : incoming) {
        		final boolean removedFromSource = getEdgeContainer(e.getSource()).removeOutgoingEdge(e);
        		assert removedFromSource;
        	}
        	getEdgeContainer(vertex).removeIncomingEdges();
        }
        
        public void removeOutgoingEdgesOf(V vertex)
        {
        	final Set<E> outgoing = getEdgeContainer(vertex).getUnmodifiableOutgoingEdges();
        	for (E e : outgoing) {
        		final boolean removedFromTarget = getEdgeContainer(e.getTarget()).removeIncomingEdge(e);
        		assert removedFromTarget;
        	}
        	getEdgeContainer(vertex).removeOutgoingEdges();
        }
        
        /**
         * @see DirectedGraph#outDegreeOf(Object)
         */
        public int outDegreeOf(V vertex)
        {
        	ArraySet<E> outgoing = ArraySet.own(getEdgeContainer(vertex).outgoing());
            return outgoing.size();
        }




        /**
         * @see DirectedGraph#outgoingEdgesOf(Object)
         */
        public Set<E> outgoingEdgesOf(V vertex)
        {
            return getEdgeContainer(vertex).getUnmodifiableOutgoingEdges();
        }
        
        /**
         * @see DirectedGraph#outgoingEdgesOf(Object)
         */
        public Set<E> outgoingEdgesOfUnsafe(V vertex)
        {
            return getEdgeContainerUnsafe(vertex).getUnmodifiableOutgoingEdges();
        }

        public boolean removeEdgeFromTouchingVertices(E e)
        {
            V source = getEdgeSource(e);
            V target = getEdgeTarget(e);

            final boolean removedFromSource = getEdgeContainer(source).removeOutgoingEdge(e);
            final boolean removedFromTarget = getEdgeContainer(target).removeIncomingEdge(e);
            
            assert removedFromSource == removedFromTarget;
            return removedFromSource;
        }

        public void removeEdgeFromTouchingVerticesUnsafe(E e)
        {
            V source = e.getSource();
            V target = e.getTarget();

            getEdgeContainerUnsafe(source).removeOutgoingEdge(e);
            getEdgeContainerUnsafe(target).removeIncomingEdge(e);
        }

        /**
         * A lazy build of edge container for specified vertex.
         *
         * @param vertex a vertex in this graph.
         *
         * @return EdgeContainer
         */
        private DirectedEdgeContainer<E,E[]> getEdgeContainer(V vertex)
        {
            assertVertexExist(vertex);

            return vertexMapDirected.compute(vertex, (v, ec) -> {

            if (ec == null) {
                ec = new ArraySetDirectedEdgeContainer<E>();
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
        private DirectedEdgeContainer<E, E[]> getEdgeContainerUnsafe(V vertex)
        {
            assert assertVertexExist(vertex);
            
            return vertexMapDirected.get(vertex);
        }
        
        public void trimToSize() {
        	// TODO: this is a hack, obviously
        	if (vertexMapDirected instanceof SimpleVectorBase) {
        		@SuppressWarnings("rawtypes")
        		SimpleVectorBase vector = (SimpleVectorBase) vertexMapDirected;
        		vector.trimToSize();
        	}
        }
    }

}

// End AbstractBaseGraph.java
