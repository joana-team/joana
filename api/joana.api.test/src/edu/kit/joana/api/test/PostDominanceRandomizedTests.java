/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.VertexFactory;
import org.jgrapht.generate.RandomGraphGenerator;
import org.jgrapht.graph.EdgeReversedGraph;
import org.junit.Test;

import edu.kit.joana.util.Pair;
import edu.kit.joana.util.collections.SimpleVector;
import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.util.graph.GraphWalker;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;
import edu.kit.joana.wala.core.graphs.MaximalPathPostDominators;
import edu.kit.joana.wala.core.graphs.MaximalPathPostDominators.IMaximalDomEdge;
import edu.kit.joana.wala.core.graphs.SinkpathPostDominators;
import edu.kit.joana.wala.core.graphs.SinkpathPostDominators.ISinkdomEdge;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class PostDominanceRandomizedTests {

	static final int seed = 48;
	static final int maxSize = 400;
	static final int nrOfTests = 10000;
	
	public static final int maxNrOfEdges(int nrNodes) {
		return (int)(((double)nrNodes) * 3);
	}
	
	private static Pair<Set<Node>, DirectedGraph<Node, Edge>> forRandom(Random random) {
		final int n = Math.max(random.nextInt(maxSize)        , 1);
		final int m = Math.min(random.nextInt(maxNrOfEdges(n)), n * n);
		
		@SuppressWarnings("serial")
		final DirectedGraph<Node, Edge> graph = new AbstractJoanaGraph<Node, Edge>(edgeFactory, () -> new SimpleVector<>(0, n), Edge.class) {};
		{
			final RandomGraphGenerator<Node, Edge> generator = new RandomGraphGenerator<>(n, m, random.nextLong());
			
			final VertexFactory<Node> vertexFactory = new VertexFactory<Node>() {
				private int id = 0;
				@Override
				public Node createVertex() {
					return new Node(id++);
				}
			};
		
			generator.generateGraph(graph, vertexFactory, null);
		}
		
		final HashSet<Node> ms; {
			final ArrayList<Node> nodes = new ArrayList<>(graph.vertexSet());
			int sizeMs = 1 + random.nextInt(5);
			ms = new HashSet<>(sizeMs);
			
			for (int i = 0; i < sizeMs; i++) {
				int id = Math.abs(random.nextInt()) % n;
				ms.add(nodes.get(id));
			}
		}
		
		return Pair.pair(ms, graph);
		
	}
	

	@Test
	public void testNtscdNticd() {
		final Random random = new Random(seed);
		for (int t = 0; t < nrOfTests; t++) {
			final Pair<Set<Node>, DirectedGraph<Node, Edge>> p = forRandom(random);
			final DirectedGraph<Node, Edge> graph = p.getSecond();
			
			
			final Map<Node, SinkpathPostDominators.Node<Node>> isinkmap;
			final DirectedGraph<SinkpathPostDominators.Node<Node>, ISinkdomEdge<SinkpathPostDominators.Node<Node>>> isinkdom; {
				final SinkpathPostDominators<Node, Edge> doms = SinkpathPostDominators.compute(graph);
				isinkdom = doms.getResult();
				isinkmap = doms.getvToNode();
			}

			final Map<Node, MaximalPathPostDominators.Node<Node>> immap;
			final DirectedGraph<MaximalPathPostDominators.Node<Node>, IMaximalDomEdge<MaximalPathPostDominators.Node<Node>>> imdom; {
				final MaximalPathPostDominators<Node, Edge> doms = MaximalPathPostDominators.compute(graph);
				imdom = doms.getResult();
				immap = doms.getvToNode();
			}
			
			for (Node n : graph.vertexSet()) {
				SinkpathPostDominators.Node<Node> nsink = isinkmap.get(n);
				MaximalPathPostDominators.Node<Node> nmax = immap.get(n);
				
				final Set<Node> msSink; { 
					final GraphWalker<SinkpathPostDominators.Node<Node>, ISinkdomEdge<SinkpathPostDominators.Node<Node>>> dfsSink = new GraphWalker<SinkpathPostDominators.Node<Node>, ISinkdomEdge<SinkpathPostDominators.Node<Node>>>(new EdgeReversedGraph<>(isinkdom)) {
						@Override
						public void discover(SinkpathPostDominators.Node<Node> node) {}
	
						@Override
						public void finish(SinkpathPostDominators.Node<Node> node) {}
						
						@Override
						public boolean traverse(SinkpathPostDominators.Node<Node> node, ISinkdomEdge<SinkpathPostDominators.Node<Node>> edge) {
							return true;
						}
					};
					msSink = dfsSink.traverseDFS(Collections.singleton(nsink)).stream().map(v -> v.getV()).collect(Collectors.toSet());
				}
				
				final Set<Node> msMax; { 
					final GraphWalker<MaximalPathPostDominators.Node<Node>, IMaximalDomEdge<MaximalPathPostDominators.Node<Node>>> dfsMax = new GraphWalker<MaximalPathPostDominators.Node<Node>, IMaximalDomEdge<MaximalPathPostDominators.Node<Node>>>(new EdgeReversedGraph<>(imdom)) {
						@Override
						public void discover(MaximalPathPostDominators.Node<Node> node) {}
	
						@Override
						public void finish(MaximalPathPostDominators.Node<Node> node) {}
						
						@Override
						public boolean traverse(MaximalPathPostDominators.Node<Node> node, IMaximalDomEdge<MaximalPathPostDominators.Node<Node>> edge) {
							return true;
						}
					};
					msMax = dfsMax.traverseDFS(Collections.singleton(nmax)).stream().map(v -> v.getV()).collect(Collectors.toSet());
				}
				
				assertTrue(msSink.containsAll(msMax));
			}
		}
	}
	
	@Test
	public void testNtscdNtscd() {
		final Random random = new Random(seed);
		for (int t = 0; t < nrOfTests; t++) {
			final Pair<Set<Node>, DirectedGraph<Node, Edge>> p = forRandom(random);
			final DirectedGraph<Node, Edge> graph = p.getSecond();
			
			
			final Map<Node, MaximalPathPostDominators.Node<Node>> immap;
			final DirectedGraph<MaximalPathPostDominators.Node<Node>, IMaximalDomEdge<MaximalPathPostDominators.Node<Node>>> imdom; {
				final MaximalPathPostDominators<Node, Edge> doms = MaximalPathPostDominators.compute(graph);
				imdom = doms.getResult();
				immap = doms.getvToNode();
			}
			
			final Map<Node, MaximalPathPostDominators.Node<Node>> immapFixed;
			final DirectedGraph<MaximalPathPostDominators.Node<Node>, IMaximalDomEdge<MaximalPathPostDominators.Node<Node>>> imdomFixed; {
				final MaximalPathPostDominators<Node, Edge> doms = MaximalPathPostDominators.compute(graph);
				imdomFixed = doms.getResult();
				immapFixed = doms.getvToNode();
			}
			
			for (Node n : graph.vertexSet()) {
				MaximalPathPostDominators.Node<Node> nmax      = immap.get(n);
				MaximalPathPostDominators.Node<Node> nmaxFixed = immapFixed.get(n);
				
				final Set<Node> msMax; { 
					final GraphWalker<MaximalPathPostDominators.Node<Node>, IMaximalDomEdge<MaximalPathPostDominators.Node<Node>>> dfsMax = new GraphWalker<MaximalPathPostDominators.Node<Node>, IMaximalDomEdge<MaximalPathPostDominators.Node<Node>>>(new EdgeReversedGraph<>(imdom)) {
						@Override
						public void discover(MaximalPathPostDominators.Node<Node> node) {}
	
						@Override
						public void finish(MaximalPathPostDominators.Node<Node> node) {}
						
						@Override
						public boolean traverse(MaximalPathPostDominators.Node<Node> node, IMaximalDomEdge<MaximalPathPostDominators.Node<Node>> edge) {
							return true;
						}
					};
					msMax = dfsMax.traverseDFS(Collections.singleton(nmax)).stream().map(v -> v.getV()).collect(Collectors.toSet());
				}
				
				final Set<Node> msMaxFixed; { 
					final GraphWalker<MaximalPathPostDominators.Node<Node>, IMaximalDomEdge<MaximalPathPostDominators.Node<Node>>> dfsMaxFixed = new GraphWalker<MaximalPathPostDominators.Node<Node>, IMaximalDomEdge<MaximalPathPostDominators.Node<Node>>>(new EdgeReversedGraph<>(imdomFixed)) {
						@Override
						public void discover(MaximalPathPostDominators.Node<Node> node) {}
	
						@Override
						public void finish(MaximalPathPostDominators.Node<Node> node) {}
						
						@Override
						public boolean traverse(MaximalPathPostDominators.Node<Node> node, IMaximalDomEdge<MaximalPathPostDominators.Node<Node>> edge) {
							return true;
						}
					};
					msMaxFixed = dfsMaxFixed.traverseDFS(Collections.singleton(nmaxFixed)).stream().map(v -> v.getV()).collect(Collectors.toSet());
				}
				
				assertEquals(msMax, msMaxFixed);
			}
		}
	}
	
	public static final class Node implements IntegerIdentifiable {
		private int id;
		
		public Node(int id) {
			this.id = id;
		}
		@Override
		public int getId() {
			return id;
		}
		@Override
		public String toString() {
			return Integer.toString(id);
		}
	}
	
	public static final class Edge implements KnowsVertices<Node> {
		private Node source;
		private Node target;
		
		public Edge(Node source, Node target) {
			this.source = source;
			this.target = target;
		}
		@Override
		public Node getSource() {
			return source;
		}
		@Override
		public Node getTarget() {
			return target;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((source == null) ? 0 : source.hashCode());
			result = prime * result + ((target == null) ? 0 : target.hashCode());
			return result;
		}
		@Override
		public String toString() {
			return "(" + source.toString() + ", " + target.toString() + ")";
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Edge))
				return false;
			Edge other = (Edge) obj;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			if (target == null) {
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			return true;
		}
		
		
	}
	private static final EdgeFactory<Node, Edge> edgeFactory = new EdgeFactory<Node, Edge>() {
		@Override
		public Edge createEdge(Node sourceVertex, Node targetVertex) {
			return new Edge(sourceVertex, targetVertex);
		}
	};


}
