/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.kit.joana.wala.eval.jmh;

import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.VertexFactory;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;
import org.jgrapht.generate.RandomGraphGenerator;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;
import edu.kit.joana.wala.core.graphs.DominanceFrontiers;
import edu.kit.joana.wala.core.graphs.NTICDGraphPostdominanceFrontiers;
import edu.kit.joana.wala.util.WriteGraphToDot;

@Fork(2)
public class MyBenchmark {
	
	@SuppressWarnings("serial")
	public static class EntryExitGraph extends SimpleDirectedGraph<Node, Edge> {
		public EntryExitGraph(EdgeFactory<Node, Edge> edgeFactory) {
			super(edgeFactory);
		}
		public Node entry;
		public Node exit;
	}
	
	@SuppressWarnings("serial")
	public static class CDG extends AbstractJoanaGraph<Node, Edge> {

	    public static CDG build(EntryExitGraph cfg, Node entry, Node exit, EdgeFactory<Node, Edge> edgeFactory) {
	        final CDG cdg = new CDG(cfg, entry, exit, edgeFactory);

	        cdg.build();

	        return cdg;
	    }

	    private final DirectedGraph<Node, Edge> cfg;
	    private final Node entry;
	    private final Node exit;

	    private CDG(final DirectedGraph<Node, Edge> cfg, Node entry, Node exit, EdgeFactory<Node, Edge> edgeFactory) {
	        super(edgeFactory, () -> new LinkedHashMap<>(), Edge.class);
	        this.cfg = cfg;
	        this.entry = entry;
	        this.exit = exit;
	    }

	    private void build() {
	        final DirectedGraph<Node, Edge> reversedCfg = new EdgeReversedGraph<Node, Edge>(cfg);
	        final DominanceFrontiers<Node, Edge> frontiers = DominanceFrontiers.compute(reversedCfg, exit);

	        for (final Node node : cfg.vertexSet()) {
	            addVertex(node);
	        }

	        addEdge(entry, exit);

	        for (final Node node : cfg.vertexSet()) {
	            for (final Node domFrontier : frontiers.getDominanceFrontier(node)) {
	                if (node != domFrontier) {
	                    // no self dependencies
	                    addEdge(domFrontier, node);
	                }
	            }
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
	
	static void addEntryExit(EntryExitGraph graph, VertexFactory<Node> vertexFactory) {
		{
			final KosarajuStrongConnectivityInspector<Node, Edge> sccInspector = new KosarajuStrongConnectivityInspector<>(graph);
			final List<Set<Node>> sccs = sccInspector.stronglyConnectedSets();
			
			final Node exitNode = vertexFactory.createVertex();
			graph.addVertex(exitNode);
			graph.exit = exitNode;
			
			for (Set<Node> scc : sccs) {
				final boolean isSink = ! scc.stream().anyMatch(
					v -> graph.outgoingEdgesOf(v).stream().anyMatch(
						e -> !scc.contains(graph.getEdgeTarget(e))
					)
				);
				if (isSink) {
					final Node s = scc.iterator().next();
					graph.addEdge(s, exitNode);
				}
			}
		}
		{
			final KosarajuStrongConnectivityInspector<Node, Edge> sccInspector = new KosarajuStrongConnectivityInspector<>(graph);
			final List<Set<Node>> sccs = sccInspector.stronglyConnectedSets();
			
			final Node entryNode = vertexFactory.createVertex();
			graph.addVertex(entryNode);
			graph.entry = entryNode;
			
			for (Set<Node> scc : sccs) {
				final boolean isSink = ! scc.stream().anyMatch(
					v -> graph.incomingEdgesOf(v).stream().anyMatch(
						e -> !scc.contains(graph.getEdgeSource(e))
					)
				);
				if (isSink) {
					final Node s = scc.iterator().next();
					graph.addEdge(entryNode, s);
				}
			}
		}
		
	}
	
	public abstract static class RandomGraphs<G> {
		public final int maxSize = 10000;
		public final int stride = 100;
		public final int seed = 42;
		
		public final List<G> randomGraphs = new LinkedList<>();
		public final EdgeFactory<Node, Edge> edgeFactory = new EdgeFactory<Node, Edge>() {
			@Override
			public Edge createEdge(Node sourceVertex, Node targetVertex) {
				return new Edge(sourceVertex, targetVertex);
			}
		};
		
		public final VertexFactory<Node> vertexFactory = new VertexFactory<MyBenchmark.Node>() {
			private int id = 0;
			@Override
			public Node createVertex() {
				return new Node(id++);
			}
		};
		
		public final int nrEdges(int nrNodes) {
			return (int)(((double)nrNodes) * 1.5);
		}
		
		public void dumpGraph(int n, DirectedGraph<Node, Edge> graph) {
			final String cfgFileName = WriteGraphToDot.sanitizeFileName(this.getClass().getSimpleName()+"-" + n + "-cfg.dot");
			try {
				WriteGraphToDot.write(graph, cfgFileName, e -> true, v -> Integer.toString(v.getId()));
			} catch (FileNotFoundException e) {
			}
		}

	}
	@State(Scope.Benchmark)
	public static class RandomGraphsArbitrary extends RandomGraphs<DirectedGraph<Node, Edge>> {
		@Setup(Level.Trial)
		public void doSetup() {
			Random random = new Random(seed);
			for (int n = 0; n < maxSize; n+=stride) {
				final RandomGraphGenerator<Node, Edge> generator = new RandomGraphGenerator<>(n, nrEdges(n), random.nextLong());
				final DirectedGraph<Node, Edge> graph = new SimpleDirectedGraph<>(edgeFactory);
				generator.generateGraph(graph, vertexFactory, null);
				
				randomGraphs.add(graph);
				dumpGraph(n, graph);
			}
		}
	}
	
	@State(Scope.Benchmark)
	public static class RandomGraphsWithUniqueExitNode extends RandomGraphs<EntryExitGraph> {
		@Setup(Level.Trial)
		public void doSetup() {
			Random random = new Random(seed);
			for (int n = 0; n < maxSize; n+=stride) {
				final RandomGraphGenerator<Node, Edge> generator = new RandomGraphGenerator<>(n, nrEdges(n), random.nextLong());
				final EntryExitGraph graph = new EntryExitGraph(edgeFactory);
				generator.generateGraph(graph, vertexFactory, null);
				
				addEntryExit(graph, vertexFactory);
				
				randomGraphs.add(graph);
				dumpGraph(n, graph);
			}
		}
	}
	
	@Benchmark
	@Warmup(iterations = 2)
	@Measurement(iterations = 1)
	@BenchmarkMode(Mode.AverageTime)
	public void testRandom(RandomGraphsArbitrary randomGraphs, Blackhole blackhole) {
		for (DirectedGraph<Node, Edge> graph : randomGraphs.randomGraphs) {
			blackhole.consume(NTICDGraphPostdominanceFrontiers.compute(graph, randomGraphs.edgeFactory, Edge.class));
		}
	}
	
	@Benchmark
	@Warmup(iterations = 2)
	@Measurement(iterations = 1)
	@BenchmarkMode(Mode.AverageTime)
	public void testClassicCDGForRandomWithUniqueExitNode(RandomGraphsWithUniqueExitNode randomGraphs, Blackhole blackhole) {
		for (EntryExitGraph graph : randomGraphs.randomGraphs) {
			blackhole.consume(CDG.build(graph, graph.entry, graph.exit, randomGraphs.edgeFactory));
		}
	}
	
	@Benchmark
	@Warmup(iterations = 2)
	@Measurement(iterations = 1)
	@BenchmarkMode(Mode.AverageTime)
	public void testNTICDGraphPostdominanceFrontiersForRandomWithUniqueExitNode(RandomGraphsWithUniqueExitNode randomGraphs, Blackhole blackhole) {
		for (EntryExitGraph graph : randomGraphs.randomGraphs) {
			blackhole.consume(NTICDGraphPostdominanceFrontiers.compute(graph, randomGraphs.edgeFactory, Edge.class));
		}
	}
	

	public static void mmain(String[] args) throws RunnerException {
		final Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
		final RandomGraphsWithUniqueExitNode randomGraphs = new RandomGraphsWithUniqueExitNode();
		randomGraphs.doSetup();
		new MyBenchmark().testClassicCDGForRandomWithUniqueExitNode(                      randomGraphs, blackhole);
		new MyBenchmark().testNTICDGraphPostdominanceFrontiersForRandomWithUniqueExitNode(randomGraphs, blackhole);
	}

	
	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
			.include(MyBenchmark.class.getSimpleName())
			.forks(1)
			.build();
		new Runner(opt).run();
	}
}
