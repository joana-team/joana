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
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.VertexFactory;
import org.jgrapht.generate.RandomGraphGenerator;
import org.junit.Test;

import edu.kit.joana.util.Pair;
import edu.kit.joana.util.collections.SimpleVector;
import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;
import edu.kit.joana.wala.core.graphs.FCACD;
import edu.kit.joana.wala.core.graphs.NTICDControlSlices;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class ControlSlicesTestsRandomized {

	static final int seed = 42;
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
	public void testWCC() {
		final Random random = new Random(seed);
		for (int t = 0; t < nrOfTests; t++) {
			final Pair<Set<Node>, DirectedGraph<Node, Edge>> p = forRandom(random);
			final DirectedGraph<Node, Edge> graph = p.getSecond();
			final Set<Node> ms = p.getFirst();
			
			final Set<Node> resultFCACD = FCACD.wcc(graph, ms);
			final Set<Node> resultNTICD = NTICDControlSlices.wcc(graph, ms, Edge.class, edgeFactory);
			
			assertEquals(resultFCACD, resultNTICD);
		}
	}
	
	@Test
	public void testWD() {
		final Random random = new Random(seed);
		for (int t = 0; t < nrOfTests; t++) {
			final Pair<Set<Node>, DirectedGraph<Node, Edge>> p = forRandom(random);
			final DirectedGraph<Node, Edge> graph = p.getSecond();
			final Set<Node> ms = p.getFirst();
			
			final Set<Node> resultFCACD = FCACD.wd(graph, ms);
			final Set<Node> resultNTICD = NTICDControlSlices.wd(graph, ms, Edge.class, edgeFactory);
			
			assertEquals(resultFCACD, resultNTICD);
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
