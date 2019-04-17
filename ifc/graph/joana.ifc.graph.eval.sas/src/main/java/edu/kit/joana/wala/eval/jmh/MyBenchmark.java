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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.VertexFactory;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;
import org.jgrapht.generate.RandomGraphGenerator;
import org.jgrapht.graph.EdgeReversedGraph;
import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.AuxCounters.Type;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import edu.kit.joana.util.collections.SimpleVector;
import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.util.graph.GraphWalker;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;
import edu.kit.joana.util.graph.LadderGraphGenerator;
import edu.kit.joana.wala.core.graphs.NTICDGraphPostdominanceFrontiers;
import edu.kit.joana.wala.core.graphs.NTIOD;
import edu.kit.joana.wala.core.graphs.SinkdomControlSlices;
import edu.kit.joana.wala.core.graphs.SinkpathPostDominators;
import edu.kit.joana.wala.core.graphs.SinkpathPostDominators.ISinkdomEdge;
import edu.kit.joana.wala.core.graphs.FCACD;
import edu.kit.joana.wala.core.graphs.NTICDControlSlices;
import edu.kit.joana.wala.util.WriteGraphToDot;

@Fork(value = 1, jvmArgsAppend = "-Xss128m")
public class MyBenchmark {
	
	@SuppressWarnings("serial")
	public static class EntryExitGraph extends AbstractJoanaGraph<Node, Edge> {
		public EntryExitGraph(EdgeFactory<Node, Edge> edgeFactory, int n) {
			super(edgeFactory, () -> new SimpleVector<>(0, n), Edge.class);
		}
		public Node entry;
		public Node exit;
	}
	
	
	public static class WeakControlClosure {
		public static Set<Node> viaNTICD(DirectedGraph<Node, Edge> graph, Set<Node> ms) {
			final Set<Node> result = NTICDControlSlices.wcc(graph, ms, Edge.class, edgeFactory);
			return result;
		}
		
		public static Set<Node> viaISINKDOM(DirectedGraph<Node, Edge> graph, Set<Node> ms) {
			final Set<Node> result = SinkdomControlSlices.wcc(graph, ms, Edge.class, edgeFactory);
			return result;
		}
		
		public static Set<Node> viaFCACD(DirectedGraph<Node, Edge> graph, Set<Node> ms) {
			final Set<Node> result = FCACD.wcc(graph, ms);
			return result;
		}
	}
	
	public static class NticdNtiod {
		public static Set<Node> viaNTIOD(DirectedGraph<Node, Edge> graph, Set<Node> ms) {
			final Set<Node> result = NTICDControlSlices.nticdNtiod(graph, ms, Edge.class, edgeFactory);
			return result;
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
	
	public static final class Edge implements KnowsVertices<Node>, Comparable<Edge> {
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
		public int compareTo(Edge o) {
			int compareSource = Integer.compare(this.getSource().getId(), o.getSource().getId());
			if (compareSource != 0) return compareSource;
			int compareTarget = Integer.compare(this.getTarget().getId(), o.getTarget().getId());
			return compareTarget;
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
	
	static void addBackEdgesToExitNodes(DirectedGraph<Node, Edge> graph) {
		final int maxDepth = 100;
		final Random r = new Random(42);
		final List<Node> exitNodes = graph.vertexSet().stream().filter(n -> graph.outgoingEdgesOf(n).isEmpty() || (graph.outgoingEdgesOf(n).size() == 1 && graph.outgoingEdgesOf(n).stream().anyMatch(e -> e.target == n))).collect(Collectors.toList());
		for (Node n : exitNodes) {
			final GraphWalker<Node, Edge> rdfs = new GraphWalker<Node, Edge>(new EdgeReversedGraph<>(graph)) {
				int depth = 0;
				
				@Override
				public void discover(Node node) { depth++;}

				@Override
				public void finish(Node node) { depth--;}
				
				@Override
				public boolean traverse(Node node, Edge edge) {
					return depth <= maxDepth;
				}
				
				@Override
				protected Iterable<Edge> newOutEdges(Set<Edge> outEdges) {
					return new TreeSet<>(outEdges);
				}
			};
			final Set<Node> toN = rdfs.traverseDFS(Collections.singleton(n));
			if (toN.size() > 1) toN.remove(n);
			final List<Node> candidates = new ArrayList<>(toN);
			candidates.sort(new Comparator<Node>() {
				@Override
				public int compare(Node o1, Node o2) {
					return Integer.compare(o1.id, o2.id);
				}
			});
			final Node m = candidates.get(r.nextInt(candidates.size()));
			@SuppressWarnings("unused")
			final Edge e = graph.addEdge(n, m);
			
		}
	}
	
	public static final EdgeFactory<Node, Edge> edgeFactory = new EdgeFactory<Node, Edge>() {
		@Override
		public Edge createEdge(Node sourceVertex, Node targetVertex) {
			return new Edge(sourceVertex, targetVertex);
		}
	};

	
	public abstract static class Graphs<G> {
		private final boolean dumpingEnabled = false;
		public ArrayList<G> graphs;
		
		public abstract int getNrOfGraphs();
		
		public final VertexFactory<Node> newVertexFactory() {
			return new VertexFactory<MyBenchmark.Node>() {
				private int id = 0;
				@Override
				public Node createVertex() {
					return new Node(id++);
				}
			};
		}
		
		public void dumpGraph(int n, int i, DirectedGraph<Node, Edge> graph) {
			if (!dumpingEnabled) return;
			final String cfgFileName = WriteGraphToDot.sanitizeFileName(this.getClass().getSimpleName()+"-" + graph.getClass().getName() + "-" + n + "-" + i +"-cfg.dot");
			try {
				WriteGraphToDot.write(graph, cfgFileName, e -> true, v -> Integer.toString(v.getId()));
			} catch (FileNotFoundException e) {
			}
			final DirectedGraph<SinkpathPostDominators.Node<Node>, ISinkdomEdge<SinkpathPostDominators.Node<Node>>> isinkdom = SinkpathPostDominators.compute(graph).getResult();
			final String isinkdomFileName = WriteGraphToDot.sanitizeFileName(this.getClass().getSimpleName()+"-" + graph.getClass().getName() + "-" + n + "-" + i +"-isinkdom.dot");
			try {
				WriteGraphToDot.write(isinkdom, isinkdomFileName, e -> true, v -> Integer.toString(v.getId()));
			} catch (FileNotFoundException e) {
			}
		}

	}
	
	@AuxCounters(Type.EVENTS)
	@State(Scope.Thread)
	public static class Size {
		public int size;
		
		@Setup(Level.Iteration)
		public void clean() {
			size = 0;
		}
	}

	
	public abstract static class RandomGraphs<G> extends Graphs<G> {
		public final int seed = 42;
		
		public abstract int nrEdges(int nrNodes);
	}
	
	@State(Scope.Benchmark)
	public static class RandomGraphsArbitrary extends RandomGraphs<DirectedGraph<Node, Edge>> {
		@Param({"500", "1000", "1500", "2000", "2500", "3000", "3500", "4000", "4500", "5000", "5500", "6000", "6500", "7000", "7500", "8000", "8500", "9000", "9500", "10000", "10500", "11000", "11500", "12000", "12500", "13000", "13500", "14000", "14500", "15000", "15500", "16000", "16500", "17000", "17500", "18000", "18500", "19000", "19500", "20000", "20500", "21000", "21500", "22000", "22500", "23000", "23500", "24000", "24500", "25000", "25500", "26000", "26500", "27000", "27500", "28000", "28500", "29000", "29500", "30000", "30500", "31000", "31500", "32000", "32500", "33000", "33500", "34000", "34500", "35000", "35500", "36000", "36500", "37000", "37500", "38000", "38500", "39000", "39500", "40000", "40500", "41000", "41500", "42000", "42500", "43000", "43500", "44000", "44500", "45000", "45500", "46000", "46500", "47000", "47500", "48000", "48500", "49000", "49500", "50000", "50500", "51000", "51500", "52000", "52500", "53000", "53500", "54000", "54500", "55000", "55500", "56000", "56500", "57000", "57500", "58000", "58500", "59000", "59500", "60000", "60500", "61000", "61500", "62000", "62500", "63000", "63500", "64000", "64500", "65000"})
		public int n;
		
		
		@Override
		public int getNrOfGraphs() {
			return 1;
		}
		
		@Override
		public int nrEdges(int nrNodes) {
			return (int)(((double)nrNodes) * 2);
		}
		
		@Setup(Level.Trial)
		public void doSetup() {
			final Random random = new Random(seed + n);
			this.graphs = new ArrayList<>();
			for (int i = 0; i < getNrOfGraphs(); i++) {
				final RandomGraphGenerator<Node, Edge> generator = new RandomGraphGenerator<>(n, nrEdges(n), random.nextLong());
				@SuppressWarnings("serial")
				final DirectedGraph<Node, Edge> graph = new AbstractJoanaGraph<Node, Edge>(edgeFactory, () -> new SimpleVector<>(0, n), Edge.class) {};
				generator.generateGraph(graph, newVertexFactory(), null);
				
				this.graphs.add(graph);
				dumpGraph(n, i, graph);
			}
			
		}
	}
	
	@State(Scope.Benchmark)
	public static class RandomGraphsArbitraryWithNodeSet extends RandomGraphs<DirectedGraph<Node, Edge>> {
		@Param({"200000", "240000", "280000", "320000", "360000", "400000"})
		public int n;
		
		@Override
		public int getNrOfGraphs() {
			return 1;
		}
		
		@Override
		public int nrEdges(int nrNodes) {
			return 2 * nrNodes;
		}
		
		private ArrayList<Set<Node>> mss;
		
		@Setup(Level.Trial)
		public void doSetup() {
			final Random random = new Random(seed + n);
			this.graphs = new ArrayList<>();
			this.mss = new ArrayList<>();
			for (int i = 0; i < getNrOfGraphs(); i++) {

				final RandomGraphGenerator<Node, Edge> generator = new RandomGraphGenerator<>(n, nrEdges(n), random.nextLong());
				@SuppressWarnings("serial")
				final DirectedGraph<Node, Edge> graph = new AbstractJoanaGraph<Node, Edge>(edgeFactory, () -> new SimpleVector<>(0, n), Edge.class) {};
				generator.generateGraph(graph, newVertexFactory(), null);
				
				this.graphs.add(graph);
				dumpGraph(n, i, graph);
				
				
				final ArrayList<Node> nodes = new ArrayList<>(graph.vertexSet());
				int sizeMs = 5;
				final HashSet<Node> ms = new HashSet<>(sizeMs);
				for (int j = 0; j < sizeMs; j++) {
					int id = Math.abs(random.nextInt()) % n;
					ms.add(nodes.get(id));
				}
				this.mss.add(ms);
			}
		}
	}
	
	@State(Scope.Benchmark)
	public static class FCACDLIkeGraphs extends RandomGraphs<DirectedGraph<Node, Edge>> {
		@Param({"500", "1000", "1500", "2000", "2500", "3000", "3500", "4000", "4500", "5000", "5500", "6000", "6500", "7000", "7500", "8000", "8500", "9000", "9500", "10000", "10500", "11000", "11500", "12000", "12500", "13000", "13500", "14000", "14500", "15000", "15500", "16000", "16500", "17000", "17500", "18000", "18500", "19000", "19500", "20000", "20500", "21000", "21500", "22000", "22500", "23000", "23500", "24000", "24500", "25000", "25500", "26000", "26500", "27000", "27500", "28000", "28500", "29000", "29500", "30000", "30500", "31000", "31500", "32000", "32500", "33000", "33500", "34000", "34500", "35000", "35500", "36000", "36500", "37000", "37500", "38000", "38500", "39000", "39500", "40000", "40500", "41000", "41500", "42000", "42500", "43000", "43500", "44000", "44500", "45000", "45500", "46000", "46500", "47000", "47500", "48000", "48500", "49000", "49500", "50000", "50500", "51000", "51500", "52000", "52500", "53000", "53500", "54000", "54500", "55000", "55500", "56000", "56500", "57000", "57500", "58000", "58500", "59000", "59500", "60000", "60500", "61000", "61500", "62000", "62500", "63000", "63500", "64000", "64500", "65000"})
		public int n;
		
		
		private ArrayList<Set<Node>> mss;
		
		@Override
		public int getNrOfGraphs() {
			return 100;
		}
		
		@Override
		public int nrEdges(int nrNodes) {
			return 2*nrNodes;
		}
		
		@Setup(Level.Trial)
		public void doSetup() {
			final Random random = new Random(seed + n);
			this.graphs = new ArrayList<>();
			this.mss = new ArrayList<>();
			
			for (int i = 0; i < getNrOfGraphs(); i++) {
				final RandomGraphGenerator<Node, Edge> generator = new RandomGraphGenerator<>(n, nrEdges(n), random.nextLong());
				@SuppressWarnings("serial")
				final DirectedGraph<Node, Edge> graph = new AbstractJoanaGraph<Node, Edge>(edgeFactory, () -> new SimpleVector<>(0, n), Edge.class) {};
				generator.generateGraph(graph, newVertexFactory(), null);
				
				this.graphs.add(graph);
				dumpGraph(n, i, graph);
				
				
				final ArrayList<Node> nodes = new ArrayList<>(graph.vertexSet());
				int sizeMs = 3;
				final HashSet<Node> ms = new HashSet<>(sizeMs);
				for (int j = 0; j < sizeMs; j++) {
					int id = Math.abs(random.nextInt()) % n;
					ms.add(nodes.get(id));
				}
				this.mss.add(ms);
			}
		}
	}

	
	@State(Scope.Benchmark)
	public static class RandomGraphsWithUniqueExitNode extends RandomGraphs<EntryExitGraph> {
		@Param({"400000", "8000", "12000", "16000", "20000", "24000", "28000", "32000", "36000", "40000"})
		public int n;

		@Override
		public int getNrOfGraphs() {
			return 1;
		}
		
		@Override
		public int nrEdges(int nrNodes) {
			return 2 * nrNodes;
		}

		
		@Setup(Level.Trial)
		public void doSetup() {
			final Random random = new Random(n + seed);
			this.graphs = new ArrayList<>();
			
			for (int i = 0; i < getNrOfGraphs(); i++) {
				final RandomGraphGenerator<Node, Edge> generator = new RandomGraphGenerator<>(n, nrEdges(n), random.nextLong());
				final EntryExitGraph graph = new EntryExitGraph(edgeFactory, n + 2);
				final VertexFactory<Node> vertexFactory = newVertexFactory();
				generator.generateGraph(graph, vertexFactory, null);
				
				addEntryExit(graph, vertexFactory);
				
				this.graphs.add(graph);
				dumpGraph(n, i, graph);
			}
		}
	}
	
	@State(Scope.Benchmark)
	public static class RandomGraphsArbitraryNoExitNodes extends RandomGraphs<DirectedGraph<Node, Edge>> {
		@Param({"30", "60", "90", "120", "150", "180", "210", "240", "270", "300", "330", "360", "390", "420", "450", "480", "510", "540", "570", "600", "630", "660", "690", "720", "750", "780", "810", "840", "870", "900", "930", "960", "990", "1020", "1050", "1080", "1110", "1140", "1170", "1200", "1230", "1260", "1290", "1320", "1350", "1380", "1410", "1440", "1470", "1500", "1530", "1560", "1590", "1620", "1650", "1680", "1710", "1740", "1770", "1800", "1830", "1860", "1890", "1920", "1950", "1980", "2010", "2040", "2070", "2100", "2130", "2160", "2190", "2220", "2250", "2280", "2310", "2340", "2370", "2400", "2430", "2460", "2490", "2520", "2550", "2580", "2610", "2640", "2670", "2700", "2730", "2760", "2790", "2820", "2850", "2880", "2910", "2940", "2970", "3000"})
		public int n;
		
		@Override
		public int nrEdges(int nrNodes) {
			return (int)(((double)nrNodes) * 1.5);
		}
		
		@Override
		public int getNrOfGraphs() {
			return 1;
		}
		
		@Setup(Level.Trial)
		public void doSetup() {
			final Random random = new Random(seed + n);
			this.graphs = new ArrayList<>();
			for (int i = 0; i < getNrOfGraphs(); i++) {
				final RandomGraphGenerator<Node, Edge> generator = new RandomGraphGenerator<>(n, nrEdges(n), random.nextLong());
				@SuppressWarnings("serial")
				final DirectedGraph<Node, Edge> graph = new AbstractJoanaGraph<Node, Edge>(edgeFactory, () -> new SimpleVector<>(0, n), Edge.class) {};
				generator.generateGraph(graph, newVertexFactory(), null);
				
				addBackEdgesToExitNodes(graph);
				
				this.graphs.add(graph);
				dumpGraph(n, i, graph);
			}
			
		}
	}

	
	@State(Scope.Benchmark)
	public static class EntryExitLadderGraph extends Graphs<EntryExitGraph> {
		@Param({"5000", "10000", "15000", "20000", "25000", "30000"})
		public int n;
		
		@Override
		public int getNrOfGraphs() {
			return 1;
		}
		
		@Setup(Level.Trial)
		public void doSetup() {
			this.graphs = new ArrayList<>();
			for (int i = 0; i < getNrOfGraphs(); i++) {
				final LadderGraphGenerator<Node, Edge> generator = new LadderGraphGenerator<>(n);
				final EntryExitGraph graph = new EntryExitGraph(edgeFactory, 2*n + 2 + 1);
				generator.generateGraph(graph, newVertexFactory(), null);
				graph.addEdge(generator.getExit1(), generator.getExit2());
				
				graph.entry = generator.getEntry();
				graph.exit  = generator.getExit2();
				this.graphs.add(graph);
				dumpGraph(n, i, graph);
			}
		}
	}

	@State(Scope.Benchmark)
	public static class FullLadderGraph extends Graphs<DirectedGraph<Node, Edge>> {
		@Param({"5000", "10000", "15000", "20000", "25000", "30000"})
		public int n;
		
		@Override
		public int getNrOfGraphs() {
			return 1;
		}
		
		@Setup(Level.Trial)
		public void doSetup() {
			this.graphs = new ArrayList<>();
			for (int i = 0; i < getNrOfGraphs(); i++) {
				final LadderGraphGenerator<Node, Edge> generator = new LadderGraphGenerator<>(n);
				@SuppressWarnings("serial")
				final DirectedGraph<Node, Edge> graph = new AbstractJoanaGraph<Node, Edge>(edgeFactory, () -> new SimpleVector<>(0, 2*n + 2 + 1), Edge.class) {};
				generator.generateGraph(graph, newVertexFactory(), null);
				graph.addEdge(generator.getExit1(), generator.getEntry());
				graph.addEdge(generator.getExit2(), generator.getEntry());
				
				this.graphs.add(graph);
				dumpGraph(n, i, graph);
			}
		}
	}

	@Benchmark
	@Warmup(iterations = 1)
	@Measurement(iterations = 1)
	@BenchmarkMode(Mode.AverageTime)
	public void testWeakControlClosureViaNTICD(RandomGraphsArbitraryWithNodeSet randomGraphs, Blackhole blackhole) {
		for (int i = 0; i < randomGraphs.getNrOfGraphs(); i++) {
			final Set<Node> result = WeakControlClosure.viaNTICD(randomGraphs.graphs.get(i), randomGraphs.mss.get(i));
			blackhole.consume(result);
		}
	}
	
	@Benchmark
	@Warmup(iterations = 1)
	@Measurement(iterations = 1)
	@BenchmarkMode(Mode.AverageTime)
	public void testWeakControlClosureViaFCACD(RandomGraphsArbitraryWithNodeSet randomGraphs, Blackhole blackhole) {
		for (int i = 0; i < randomGraphs.getNrOfGraphs(); i++) {
			final Set<Node> result = WeakControlClosure.viaFCACD(randomGraphs.graphs.get(i), randomGraphs.mss.get(i));
			blackhole.consume(result);
		}
	}

	
	
	@Benchmark
	@Warmup(iterations = 1, time = 3)
	@Measurement(iterations = 1, time = 3)
	@BenchmarkMode(Mode.AverageTime)
	public void countNTIODSize(RandomGraphsArbitraryNoExitNodes randomGraphs, Size size, Blackhole blackhole) {
		for (int i = 0; i < randomGraphs.getNrOfGraphs(); i++) {
			final Map<Node, Map<Node, Set<Node>>> ntiod = NTIOD.compute(randomGraphs.graphs.get(i), edgeFactory, Edge.class);
			blackhole.consume(ntiod);
			if (size.size == 0) {
				int sizeNTIOD = ntiod.values().stream().map(m2 -> m2.values().stream().map(ns -> ns.size()).reduce(0, Integer::sum)).reduce(0, Integer::sum);
				size.size = sizeNTIOD;
			}
		}
	}

	@Benchmark
	@Warmup(iterations = 1, time = 3)
	@Measurement(iterations = 1, time = 3)
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	@BenchmarkMode(Mode.AverageTime)
	public void testFullWeakControlClosureViaNTICD(FCACDLIkeGraphs randomGraphs, Blackhole blackhole) {
		for (int i = 0; i < randomGraphs.getNrOfGraphs(); i++) {
			final Set<Node> result = WeakControlClosure.viaNTICD(randomGraphs.graphs.get(i), randomGraphs.mss.get(i));
			blackhole.consume(result);
		}
	}
	
	@Benchmark
	@Warmup(iterations = 1, time = 3)
	@Measurement(iterations = 1, time = 3)
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	@BenchmarkMode(Mode.AverageTime)
	public void testFullWeakControlClosureViaFCACD(FCACDLIkeGraphs randomGraphs, Blackhole blackhole) {
		for (int i = 0; i < randomGraphs.getNrOfGraphs(); i++) {
			final Set<Node> result = WeakControlClosure.viaFCACD(randomGraphs.graphs.get(i), randomGraphs.mss.get(i));
			blackhole.consume(result);
		}
	}
	
	@Benchmark
	@Warmup(iterations = 1, time = 3)
	@Measurement(iterations = 1, time = 3)
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	@BenchmarkMode(Mode.AverageTime)
	public void testFullWeakControlClosureViaISINKDOM(FCACDLIkeGraphs randomGraphs, Blackhole blackhole) {
		for (int i = 0; i < randomGraphs.getNrOfGraphs(); i++) {
			final Set<Node> result = WeakControlClosure.viaISINKDOM(randomGraphs.graphs.get(i), randomGraphs.mss.get(i));
			blackhole.consume(result);
		}
	}
	
	@Benchmark
	@Warmup(iterations = 1, time = 5)
	@Measurement(iterations = 1, time = 5)
	@BenchmarkMode(Mode.AverageTime)
	public void testRandom(RandomGraphsArbitrary randomGraphs, Blackhole blackhole) {
		for (int i = 0; i < randomGraphs.getNrOfGraphs(); i++) {
			final DirectedGraph<Node, Edge> graph = randomGraphs.graphs.get(i);
			blackhole.consume(NTICDGraphPostdominanceFrontiers.compute(graph, edgeFactory, Edge.class));
		}
	}
	
	@Benchmark
	@Warmup(iterations = 1, time = 5)
	@Measurement(iterations = 1, time = 5)
	@BenchmarkMode(Mode.AverageTime)
	public void testNTICDGraphPostdominanceFrontiersForRandomWithUniqueExitNode(RandomGraphsWithUniqueExitNode randomGraphs, Blackhole blackhole) {
		for (int i = 0; i < randomGraphs.getNrOfGraphs(); i++) {
			final EntryExitGraph graph = randomGraphs.graphs.get(i);
			blackhole.consume(NTICDGraphPostdominanceFrontiers.compute(graph, edgeFactory, Edge.class));
		}
	}
	

	@Benchmark
	@Warmup(iterations = 1, time = 5)
	@Measurement(iterations = 1, time = 5)
	@BenchmarkMode(Mode.AverageTime)
	public void testNTICDGraphPostdominanceFrontiersForEntryExitLadder(EntryExitLadderGraph ladderGraphs, Blackhole blackhole) {
		for (int i = 0; i < ladderGraphs.getNrOfGraphs(); i++) {
			final DirectedGraph<Node, Edge> graph = ladderGraphs.graphs.get(i);
			blackhole.consume(NTICDGraphPostdominanceFrontiers.compute(graph, edgeFactory, Edge.class));
		}
	}
	
	@Benchmark
	@Warmup(iterations = 1, time = 5)
	@Measurement(iterations = 1, time = 5)
	@BenchmarkMode(Mode.AverageTime)
	public void testSinkPostdominanceFrontiersForEntryExitLadder(EntryExitLadderGraph ladderGraphs, Blackhole blackhole) {
		for (int i = 0; i < ladderGraphs.getNrOfGraphs(); i++) {
			final DirectedGraph<Node, Edge> graph = ladderGraphs.graphs.get(i);
			blackhole.consume(SinkpathPostDominators.compute(graph));
		}
	}
	
	public static void mainDebug(String[] args) throws RunnerException {
	//public static void main(String[] args) throws RunnerException {
		final Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
		final RandomGraphsArbitraryWithNodeSet randomGraphs = new RandomGraphsArbitraryWithNodeSet();
		randomGraphs.n = 62;
		randomGraphs.doSetup();
		System.out.println(randomGraphs.mss);
		new MyBenchmark().testWeakControlClosureViaNTICD(                                 randomGraphs, blackhole);
	}
	
	//public static void mainPrintParam(String[] args) {
	public static void main(String[] args) {
		final int nr     = 100;
		final int stride = 30; 
		boolean isFirst = true;
		System.out.print("@Param({");
		for (int i = 1; i <= nr; i++) {
			if (!isFirst) System.out.print(", ");
			isFirst = false;
			System.out.print("\""+(i*stride)+"\"");
		}
		System.out.println("})");
	}

	public static void mainManual(String[] args) throws RunnerException {
	//public static void main(String[] args) throws RunnerException {		
		Options opt = new OptionsBuilder()
			.include(MyBenchmark.class.getSimpleName())
			.forks(1)
			.build();
		new Runner(opt).run();
	}
}
