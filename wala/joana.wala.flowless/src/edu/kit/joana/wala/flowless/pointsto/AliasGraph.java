/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.pointsto;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.graph.impl.DelegatingNumberedNodeManager;
import com.ibm.wala.util.graph.impl.SparseNumberedEdgeManager;
import com.ibm.wala.util.graph.impl.SparseNumberedGraph;

import edu.kit.joana.wala.flowless.pointsto.PtsParameter.RootParameter;

/**
 * An alias graph describes an aliasing configuration of the parameter and fields
 * of a single method. The aliasing may either be described as a may-alias graph
 * or a not-may-alias graph. Parameters that are connected in a may-alias graph
 * may point to the same location in memory, while params connected in a not-may-alias
 * graph definitely do not point to the same location.
 * Note that an edge between two parameters does NOT mean that they may be stored at
 * the same location, but that thier value may point to the same location.
 * As primitive types do not point to any location, edges between them do not
 * make sense. However for the sake of a simpler computation algorithm we
 * allow primitive typed parameter to be connected, but the edges have to resemble
 * the aliasing of their parents.
 *
 * E.g. assume field i to be a primitive type: (a.b.i <-> c.b.i) <=> (a.b <-> c.b)
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public abstract class AliasGraph extends AbstractNumberedGraph<PtsParameter> implements Cloneable {

	/**
	 * Describes the aliasing configuration of the parameters and fields of a
	 * single method. Elements that are directly connected in this graph, may point
	 * to the same location.
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class MayAliasGraph extends AliasGraph implements PartialOrder<MayAliasGraph> {

		/**
		 * Creates a new instance of a may-alias graph.
		 */
		public MayAliasGraph(final boolean isStaticMethod) {
			super(false, isStaticMethod);
		}

		/**
		 * Constructs the matching not-may-alias graph for this may-alias graph.
		 * Calling constructNegated again on the resulting NoMayAliasGraph should
		 * return a graph equal to this.
		 *
		 * @return A not-may-alias graph that describes the same aliasing configuration
		 * as this may-alias graph.
		 */
		public NoMayAliasGraph constructNegated() {
			return (NoMayAliasGraph) AliasGraph.negate(this);
		}

		/**
		 * @see edu.kit.joana.wala.flowless.pointsto.AliasGraph#clone()
		 */
		public MayAliasGraph clone() {
			MayAliasGraph clone = new MayAliasGraph(isStaticMethod());

			for (PtsParameter node : this) {
				clone.addNode(node);
			}

			for (PtsParameter from : this) {
				for (Iterator<PtsParameter> it = getSuccNodes(from); it.hasNext();) {
					PtsParameter to = it.next();
					clone.addEdge(from, to);
				}
			}

			return clone;
		}

		public void verify() throws AliasGraphException {
			// check if aliasing is propagated from parent to children
			// iff two nodes are aliased, their fields have to be aliased too.
			for (PtsParameter root1 : getRoots()) {
				for (PtsParameter root2 : getRoots()) {
					if (root1 == root2) {
						continue;
					}

					if (hasEdge(root1, root2)) {
						checkAllChildrenConnected(root1, root2);
					} else {
						verifyChildren(root1, root2);
					}
				}
			}

			// isn't this already checked by the algorithm above?
			//TODO check propagation of not-aliasing upwards the tree from
			// child to parent. Iff two nodes are not aliased, their parent
			// are definitely not aliased too.
		}

		private void verifyChildren(PtsParameter parent1, PtsParameter parent2) throws AliasGraphException {
			for (PtsParameter child1 : parent1.getChildren()) {
				for (PtsParameter child2 : parent2.getChildren()) {
					if (child1.getName().equals(child2.getName())
							&& child1.getType().equals(child2.getType())) {
						if (hasEdge(child1, child2)) {
							checkAllChildrenConnected(child1, child2);
						} else {
							verifyChildren(child1, child2);
						}
					}
				}
			}
		}

		private void checkAllChildrenConnected(PtsParameter parent1, PtsParameter parent2) throws AliasGraphException {
			for (PtsParameter child1 : parent1.getChildren()) {
				for (PtsParameter child2 : parent2.getChildren()) {
					if (child1.getName().equals(child2.getName())
							&& child1.getType().equals(child2.getType())) {

						if (!hasEdge(child1, child2)) {
							throw new AliasGraphException("alias(" + parent1 + ", " + parent2 + ") found, "
									+ "but their children are not aliased: !alias(" + child1 + ", " + child2 + ")");
						}
					}
				}
			}
		}

		private static class Pair<T> {

			public final T first;
			public final T second;

			public Pair(T first, T second) {
				this.first = first;
				this.second = second;
			}

			public String toString() {
				return "(" + first + ", " + second + ")";
			}

		}

		@Override
		public Cmp compareTo(final MayAliasGraph o) {
			if (o == this) {
				return Cmp.EQUAL;
			}

			final List<Pair<RootParameter>> pairs = new LinkedList<Pair<RootParameter>>();
			final Set<RootParameter> matched = new HashSet<RootParameter>();

			for (RootParameter root : getRoots()) {
				for (RootParameter otherRoot : o.getRoots()) {
					if (root.equals(otherRoot)) {
						final Pair<RootParameter> pair = new Pair<RootParameter>(root, otherRoot);
						pairs.add(pair);
						matched.add(root);
						matched.add(otherRoot);
						break;
					}
				}
			}

			boolean thisIsBigger = false;
			boolean otherIsBigger = false;

			// check pairs
			for (Pair<RootParameter> pair : pairs) {
				final Cmp paramCmp = compare(this, pair.first, o, pair.second);

				switch (paramCmp) {
				case BIGGER:
					thisIsBigger = true;
					break;
				case SMALLER:
					otherIsBigger = true;
					break;
				case EQUAL:
					// nothing to do
					break;
				case UNCOMPARABLE:
					return Cmp.UNCOMPARABLE;
				}
			}

			// check no aliasing from or to unmatched params
			for (RootParameter p : getRoots()) {
				if (!matched.contains(p) && containsAlias(this, p)) {
					return Cmp.UNCOMPARABLE;
				}
			}

			for (RootParameter p : o.getRoots()) {
				if (!matched.contains(p) && containsAlias(o, p)) {
					return Cmp.UNCOMPARABLE;
				}
			}

			if (thisIsBigger && otherIsBigger) {
				return Cmp.UNCOMPARABLE;
			} else if (thisIsBigger && !otherIsBigger) {
				return Cmp.BIGGER;
			} else if (!thisIsBigger && otherIsBigger) {
				return Cmp.SMALLER;
			} else {
				return Cmp.EQUAL;
			}
		}

		public static boolean containsAlias(MayAliasGraph g, PtsParameter p) {
			if (g.getSuccNodeCount(p) > 0) {
				return true;
			}

			for (PtsParameter ch : p.getChildren()) {
				if (containsAlias(g, ch)) {
					return true;
				}
			}

			return false;
		}

		public static Cmp compare(final MayAliasGraph g1, final PtsParameter p1, final MayAliasGraph g2, final PtsParameter p2) {
			boolean g1Bigger = false;
			boolean g2Bigger = false;

			Set<PtsParameter> g1Succ = new HashSet<PtsParameter>();

			for (Iterator<PtsParameter> it = g1.getSuccNodes(p1); it.hasNext(); ) {
				PtsParameter succ = it.next();
				g1Succ.add(succ);
			}

			Set<PtsParameter> g2Succ = new HashSet<PtsParameter>();

			for (Iterator<PtsParameter> it = g2.getSuccNodes(p2); it.hasNext(); ) {
				PtsParameter succ = it.next();
				g2Succ.add(succ);
			}

			Set<PtsParameter> copy = new HashSet<PtsParameter>(g1Succ);
			copy.removeAll(g2Succ);

			if (!copy.isEmpty()) {
				g1Bigger = true;
			}

			copy = new HashSet<PtsParameter>(g2Succ);
			copy.removeAll(g1Succ);

			if (!copy.isEmpty()) {
				g2Bigger = true;
			}

			for (final PtsParameter ch1 : p1.getChildren()) {
				final PtsParameter ch2 = p2.getChild(ch1.getName());
				if (ch2 == null) {
					if (containsAlias(g1, ch1)) {
						// when g2 does not have a child with the name of ch1, then ch1 may not be alias to anything
						g1Bigger = true;

						// shortcut
						if (g2Bigger) {
							return Cmp.UNCOMPARABLE;
						}
					}
					continue;
//					return Cmp.UNCOMPARABLE;
				}

				final Cmp childCmp = compare(g1, ch1, g2, ch2);

				switch (childCmp) {
				case BIGGER:
					g1Bigger = true;
					// shortcut
					if (g2Bigger) {
						return Cmp.UNCOMPARABLE;
					}
					break;
				case SMALLER:
					g2Bigger = true;
					// shortcut
					if (g1Bigger) {
						return Cmp.UNCOMPARABLE;
					}
					break;
				case EQUAL:
					// nothing to do
					break;
				case UNCOMPARABLE:
					return Cmp.UNCOMPARABLE;
				}
			}

			for (PtsParameter ch2 : p2.getChildren()) {
				PtsParameter ch1 = p1.getChild(ch2.getName());
				if (ch1 == null) {
					if (containsAlias(g2, ch2)) {
						g2Bigger = true;

						// shortcut
						if (g1Bigger) {
							return Cmp.UNCOMPARABLE;
						}
					}
				}
			}

			if (g1Bigger && g2Bigger) {
				return Cmp.UNCOMPARABLE;
			} else if (g1Bigger && !g2Bigger) {
				return Cmp.BIGGER;
			} else if (!g1Bigger && g2Bigger) {
				return Cmp.SMALLER;
			} else {
				return Cmp.EQUAL;
			}
		}
	}

	/**
	 * Describes the aliasing configuration of the parameters and fields of a
	 * single method. Elements that are directly connected in this graph, are
	 * guaranteed to not point to the same location.
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class NoMayAliasGraph extends AliasGraph implements PartialOrder<NoMayAliasGraph> {

		/**
		 * Creates a new instance of a may-alias graph.
		 */
		public NoMayAliasGraph(final boolean isStaticMethod) {
			super(true, isStaticMethod);
		}

		/**
		 * Constructs the matching may-alias graph for this not-may-alias graph.
		 * Calling constructNegated again on the resulting MayAliasGraph should
		 * return a graph equal to this.
		 *
		 * @return A may-alias graph that describes the same aliasing configuration
		 * as this not-may-alias graph.
		 */
		public MayAliasGraph constructNegated() {
			return (MayAliasGraph) AliasGraph.negate(this);
		}

		/**
		 * @see edu.kit.joana.wala.flowless.pointsto.AliasGraph#clone()
		 */
		public NoMayAliasGraph clone() {
			NoMayAliasGraph clone = new NoMayAliasGraph(isStaticMethod());

			for (PtsParameter node : this) {
				clone.addNode(node);
			}

			for (PtsParameter from : this) {
				for (Iterator<PtsParameter> it = getSuccNodes(from); it.hasNext();) {
					PtsParameter to = it.next();
					clone.addEdge(from, to);
				}
			}

			return clone;
		}

		public void verify() throws AliasGraphException {
			// lazy ass solution...
			constructNegated().verify();
		}

		@Override
		public Cmp compareTo(NoMayAliasGraph o) {
			// TODO Auto-generated method stub
			throw new UnimplementedError();
		}

	}

	private final NumberedNodeManager<PtsParameter> nodes = new DelegatingNumberedNodeManager<PtsParameter>();
	private final NumberedEdgeManager<PtsParameter> edges = new SparseNumberedEdgeManager<PtsParameter>(nodes);

	private final boolean edgeMeansNoAlias;
	/**
	 * Needed to read in the parameter numbers and interpret them correctly. See ParamNum class for details.
	 */
	private final boolean matchingMethodIsStatic;

	private AliasGraph(boolean edgeMeansNoAlias, boolean isStatic) {
		this.edgeMeansNoAlias = edgeMeansNoAlias;
		this.matchingMethodIsStatic = isStatic;
	}

	/**
	 * Needed to read in the parameter numbers and interpret them correctly. See ParamNum class for details.
	 */
	public boolean isStaticMethod() {
		return matchingMethodIsStatic;
	}
	
	/**
	 * Returns true if an edge between two nodes means that they are definitely not aliased.
	 * @return true if an edge between two nodes means that they are definitely not aliased.
	 */
	public boolean isEdgeNoMayAlias() {
		return edgeMeansNoAlias;
	}

	/**
	 * Returns true if an edge between two nodes means that they are may point to
	 * the same location - are may-aliased.
	 * @return true if an edge between two nodes means that they are may point to
	 * the same location - are may-aliased.
	 */
	public boolean isEdgeMayAlias() {
		return !edgeMeansNoAlias;
	}

	/**
	 * Creates a may-alias graph from an not-may-alias graph and vice versa.
	 * The aliasing configuration specified by this graph however does not change.
	 * @return A AliasGraph containing edges between nodes where this graph had none.
	 */
	public abstract AliasGraph constructNegated();

	@Override
	protected NumberedEdgeManager<PtsParameter> getEdgeManager() {
		return edges;
	}

	@Override
	protected NumberedNodeManager<PtsParameter> getNodeManager() {
		return nodes;
	}

	private static AliasGraph negate(AliasGraph graph) {
		AliasGraph negated = (graph.edgeMeansNoAlias 
				? new MayAliasGraph(graph.matchingMethodIsStatic)
				: new NoMayAliasGraph(graph.matchingMethodIsStatic));
		for (PtsParameter p : graph) {
			negated.addNode(p);
		}

		for (PtsParameter p : graph) {
			for (PtsParameter o : graph) {
				if (p != o) {
					negated.addEdge(p, o);
				}
			}

			Iterator<PtsParameter> succs = graph.getSuccNodes(p);
			while (succs.hasNext()) {
				PtsParameter toRemove = succs.next();
				negated.removeEdge(p, toRemove);
			}
		}

		return negated;
	}

	/**
	 * @see com.ibm.wala.util.graph.AbstractGraph#addNode(java.lang.Object)
	 */
	public void addNode(PtsParameter param) {
		super.addNode(param);

		if (param.isRoot()) {
			roots.add((RootParameter) param);
		}
	}

	private final Set<RootParameter> roots = new HashSet<RootParameter>();

	public static class AliasGraphException extends Exception {

		private static final long serialVersionUID = -1753821613332108910L;

		public AliasGraphException(String msg) {
			super(msg);
		}
	}

	public abstract void verify() throws AliasGraphException;

	/**
	 * Returns all RootParameter nodes contained in this graph. Root parameter
	 * nodes correspond to method parameters, exception or return values. All other
	 * parameter nodes refer to object field, which are reacheble through one of these
	 * root nodes.
	 * @return A collection of root nodes contained in this graph.
	 */
	public Collection<RootParameter> getRoots() {
		return Collections.unmodifiableCollection(roots);
	}

	/**
	 * Removes all edges contained in the parameter graph from this graph.
	 * @param graph A graph that defines which edges should be removed from the
	 * current graph.
	 */
	public void removeEdges(AliasGraph graph) {
		for (PtsParameter from : graph) {
			if (containsNode(from)) {
				for (Iterator<PtsParameter> it = graph.getSuccNodes(from); it.hasNext();) {
					PtsParameter to = it.next();
					if (containsNode(to) && hasEdge(from, to)) {
						removeEdge(from, to);
					}
				}
			}
		}
	}

	/**
	 * Adds all edges contained in the given graph to this graph.
	 * @param graph All edges in this graph are added to the current graph.
	 */
	public void addEdges(AliasGraph graph) {
		for (PtsParameter from : graph) {
			if (containsNode(from)) {
				for (Iterator<PtsParameter> it = graph.getSuccNodes(from); it.hasNext();) {
					PtsParameter to = it.next();
					if (containsNode(to) && !hasEdge(from, to)) {
						addEdge(from, to);
					}
				}
			}
		}
	}

	/**
	 * Creates a copy of this graph. Nodes are not cloned. Soe changes to nodes
	 * have side-effects to all other clones.
	 * @return A copy of this graph. Nodes are not cloned. Soe changes to nodes
	 * have side-effects to all other clones.
	 */
	public AliasGraph clone() {
		throw new UnimplementedError();
	}

	public void merge(final MayAliasGraph mergeFrom) {
		throw new UnimplementedError();
	}

	/**
	 * Returns true iff the two given graphs share at least a single edge.
	 * @param g1 The first graph.
	 * @param g2 The second graph.
	 * @return true iff the two given graphs share at least a single edge.
	 */
	public static boolean shareEdges(AliasGraph g1, AliasGraph g2) {
		for (PtsParameter p1 : g1) {
			for (PtsParameter p2 : g2) {
				if (g1.containsNode(p2) && g2.containsNode(p1)) {
					if ((g1.hasEdge(p1, p2) && g2.hasEdge(p1, p2))
							|| (g1.hasEdge(p2, p1) && g2.hasEdge(p2, p1))) {
						return true;
					}
				}
			}
		}

		return false;
	}

//	public static Graph<PtsParameter> computeSharedEdges(AliasGraph g1, AliasGraph g2) {
//		Graph<PtsParameter> graph = new SparseNumberedGraph<PtsParameter>();
//
//		for (PtsParameter p1 : g1) {
//			for (PtsParameter p2 : g2) {
//				if (g1.containsNode(p2) && g2.containsNode(p1)) {
//					if (g1.hasEdge(p1, p2) && g2.hasEdge(p1, p2)) {
//						if (!graph.containsNode(p1)) {
//							graph.addNode(p1);
//						}
//
//						if (!graph.containsNode(p2)) {
//							graph.addNode(p2);
//						}
//
//						graph.addEdge(p1, p2);
//					}
//
//					if (g1.hasEdge(p2, p1) && g2.hasEdge(p2, p1)) {
//						if (!graph.containsNode(p1)) {
//							graph.addNode(p1);
//						}
//
//						if (!graph.containsNode(p2)) {
//							graph.addNode(p2);
//						}
//
//						graph.addEdge(p2, p1);
//					}
//				}
//			}
//		}
//
//		return graph;
//	}

	/**
	 * Computes the intersection of two graphs. Only nodes and edges contained
	 * in both graphs are part of the intersection. This implementation may be used
	 * to compute the intersection between an may-alias and an not-may-alias graph,
	 * which is not allowed by the intersect() method.
	 * @param one The first graph.
	 * @param two The second graph.
	 * @return An intersection of the two given graphs.
	 */
	public static Graph<PtsParameter> intersectGeneric(AliasGraph one, AliasGraph two) {
		Graph<PtsParameter> intersection = new SparseNumberedGraph<PtsParameter>();

		for (PtsParameter from : one) {
			if (two.containsNode(from)) {
				for (Iterator<PtsParameter> it = one.getSuccNodes(from); it.hasNext();) {
					PtsParameter to = it.next();
					if (two.containsNode(to) && two.hasEdge(from, to)) {
						intersection.addNode(from);
						intersection.addNode(to);
						intersection.addEdge(from, to);
					}
				}
			}
		}

		return intersection;
	}


	/**
	 * Computes the intersection of two graphs. Only nodes and edges contained
	 * in both graphs are part of the intersection.
	 * @param one The first graph.
	 * @param two The second graph.
	 * @return An intersection of the two given graphs.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends AliasGraph>  T intersect(T one, T two) {
		T intersection = (T) two.clone();

		for (PtsParameter from : intersection) {
			for (Iterator<PtsParameter> it = intersection.getSuccNodes(from); it.hasNext();) {
				PtsParameter to = it.next();
				if (!one.containsNode(from) || !one.containsNode(to) || !one.hasEdge(from, to)) {
					intersection.removeEdge(from, to);
				}
			}
		}

		return intersection;
	}
}
