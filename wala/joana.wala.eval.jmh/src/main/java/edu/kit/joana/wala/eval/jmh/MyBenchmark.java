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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
import edu.kit.joana.wala.core.graphs.DominanceFrontiers;
import edu.kit.joana.wala.core.graphs.EfficientDominators;
import edu.kit.joana.wala.core.graphs.NTICDGraphPostdominanceFrontiers;
import edu.kit.joana.wala.core.graphs.NTICDMyWod;
import edu.kit.joana.wala.core.graphs.SinkdomControlSlices;
import edu.kit.joana.wala.core.graphs.SinkpathPostDominators;
import edu.kit.joana.wala.core.graphs.EfficientDominators.DomTree;
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
	        super(edgeFactory, () -> new LinkedHashMap<>(cfg.vertexSet().size()), Edge.class);
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
	
	public static class ClassicPostdominance {

		public static DomTree<Node> classicPostdominance(EntryExitGraph cfg, Node entry, Node exit) {
			final DirectedGraph<Node, Edge> reversedCfg = new EdgeReversedGraph<Node, Edge>(cfg);
			final EfficientDominators<Node, Edge> dom = EfficientDominators.compute(reversedCfg, exit);

			return dom.getDominationTree();
		}
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
	
	public static class NticdMyWod {
		public static Set<Node> viaMYWOD(DirectedGraph<Node, Edge> graph, Set<Node> ms) {
			final Set<Node> result = NTICDControlSlices.nticdMyWod(graph, ms, Edge.class, edgeFactory);
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
			};
			final Set<Node> toN = rdfs.traverseDFS(Collections.singleton(n));
			if (toN.size() > 1) toN.remove(n);
			final List<Node> candidates = new ArrayList<>(rdfs.traverseDFS(Collections.singleton(n)));
			candidates.sort(new Comparator<Node>() {
				@Override
				public int compare(Node o1, Node o2) {
					return Integer.compare(o1.id, o2.id);
				}
			});
			final Node m = candidates.get(r.nextInt(toN.size()));
			@SuppressWarnings("unused")
			final Edge e = graph.addEdge(n, m);
			//System.out.print(e + "  ");
			
		}
		//System.out.println();
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
		
		public int nrEdges(int nrNodes) {
			return (int)(((double)nrNodes) * 1.3);
		}
	}
	
	@State(Scope.Benchmark)
	public static class RandomGraphsArbitrary extends RandomGraphs<DirectedGraph<Node, Edge>> {
		@Param({"400000", "8000", "12000", "16000", "20000", "24000", "28000", "32000", "36000", "40000"})
		public int n;
		
		
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
				
				this.graphs.add(graph);
				dumpGraph(n, i, graph);
			}
			
		}
	}
	
	@State(Scope.Benchmark)
	public static class RandomGraphsArbitraryWithNodeSet extends RandomGraphs<DirectedGraph<Node, Edge>> {
		//@Param({"400000", "8000", "12000", "16000", "20000", "24000", "28000", "32000", "36000", "40000"})
		@Param({"200000", "240000", "280000", "320000", "360000", "400000"})
		public int n;
		
		@Override
		public int getNrOfGraphs() {
			return 1;
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
		//@Param({"10", "20", "30", "40", "50", "60", "70", "80", "90", "100", "110", "120", "130", "140", "150", "160", "170", "180", "190", "200", "210", "220", "230", "240", "250", "260", "270", "280", "290", "300", "310", "320", "330", "340", "350", "360", "370", "380", "390", "400", "410", "420", "430", "440", "450", "460", "470", "480", "490", "500", "510", "520", "530", "540", "550", "560", "570", "580", "590", "600", "610", "620", "630", "640", "650", "660", "670", "680", "690", "700", "710", "720", "730", "740", "750", "760", "770", "780", "790", "800", "810", "820", "830", "840", "850", "860", "870", "880", "890", "900", "910", "920", "930", "940", "950", "960", "970", "980", "990", "1000", "1010", "1020", "1030", "1040", "1050", "1060", "1070", "1080", "1090", "1100", "1110", "1120", "1130", "1140", "1150", "1160", "1170", "1180", "1190", "1200", "1210", "1220", "1230", "1240", "1250", "1260", "1270", "1280", "1290", "1300", "1310", "1320", "1330", "1340", "1350", "1360", "1370", "1380", "1390", "1400", "1410", "1420", "1430", "1440", "1450", "1460", "1470", "1480", "1490", "1500", "1510", "1520", "1530", "1540", "1550", "1560", "1570", "1580", "1590", "1600", "1610", "1620", "1630", "1640", "1650", "1660", "1670", "1680", "1690", "1700", "1710", "1720", "1730", "1740", "1750", "1760", "1770", "1780", "1790", "1800", "1810", "1820", "1830", "1840", "1850", "1860", "1870", "1880", "1890", "1900", "1910", "1920", "1930", "1940", "1950", "1960", "1970", "1980", "1990", "2000", "2010", "2020", "2030", "2040", "2050", "2060", "2070", "2080", "2090", "2100", "2110", "2120", "2130", "2140", "2150", "2160", "2170", "2180", "2190", "2200", "2210", "2220", "2230", "2240", "2250", "2260", "2270", "2280", "2290", "2300", "2310", "2320", "2330", "2340", "2350", "2360", "2370", "2380", "2390", "2400", "2410", "2420", "2430", "2440", "2450", "2460", "2470", "2480", "2490", "2500", "2510", "2520", "2530", "2540", "2550", "2560", "2570", "2580", "2590", "2600", "2610", "2620", "2630", "2640", "2650", "2660", "2670", "2680", "2690", "2700", "2710", "2720", "2730", "2740", "2750", "2760", "2770", "2780", "2790", "2800", "2810", "2820", "2830", "2840", "2850", "2860", "2870", "2880", "2890", "2900", "2910", "2920", "2930", "2940", "2950", "2960", "2970", "2980", "2990", "3000", "3010", "3020", "3030", "3040", "3050", "3060", "3070", "3080", "3090", "3100", "3110", "3120", "3130", "3140", "3150", "3160", "3170", "3180", "3190", "3200", "3210", "3220", "3230", "3240", "3250", "3260", "3270", "3280", "3290", "3300", "3310", "3320", "3330", "3340", "3350", "3360", "3370", "3380", "3390", "3400", "3410", "3420", "3430", "3440", "3450", "3460", "3470", "3480", "3490", "3500", "3510", "3520", "3530", "3540", "3550", "3560", "3570", "3580", "3590", "3600", "3610", "3620", "3630", "3640", "3650", "3660", "3670", "3680", "3690", "3700", "3710", "3720", "3730", "3740", "3750", "3760", "3770", "3780", "3790", "3800", "3810", "3820", "3830", "3840", "3850", "3860", "3870", "3880", "3890", "3900", "3910", "3920", "3930", "3940", "3950", "3960", "3970", "3980", "3990", "4000", "4010", "4020", "4030", "4040", "4050", "4060", "4070", "4080", "4090", "4100", "4110", "4120", "4130", "4140", "4150", "4160", "4170", "4180", "4190", "4200", "4210", "4220", "4230", "4240", "4250", "4260", "4270", "4280", "4290", "4300", "4310", "4320", "4330", "4340", "4350", "4360", "4370", "4380", "4390", "4400", "4410", "4420", "4430", "4440", "4450", "4460", "4470", "4480", "4490", "4500", "4510", "4520", "4530", "4540", "4550", "4560", "4570", "4580", "4590", "4600", "4610", "4620", "4630", "4640", "4650", "4660", "4670", "4680", "4690", "4700", "4710", "4720", "4730", "4740", "4750", "4760", "4770", "4780", "4790", "4800", "4810", "4820", "4830", "4840", "4850", "4860", "4870", "4880", "4890", "4900", "4910", "4920", "4930", "4940", "4950", "4960", "4970", "4980", "4990", "5000", "5010", "5020", "5030", "5040", "5050", "5060", "5070", "5080", "5090", "5100", "5110", "5120", "5130", "5140", "5150", "5160", "5170", "5180", "5190", "5200", "5210", "5220", "5230", "5240", "5250", "5260", "5270", "5280", "5290", "5300", "5310", "5320", "5330", "5340", "5350", "5360", "5370", "5380", "5390", "5400", "5410", "5420", "5430", "5440", "5450", "5460", "5470", "5480", "5490", "5500", "5510", "5520", "5530", "5540", "5550", "5560", "5570", "5580", "5590", "5600", "5610", "5620", "5630", "5640", "5650", "5660", "5670", "5680", "5690", "5700", "5710", "5720", "5730", "5740", "5750", "5760", "5770", "5780", "5790", "5800", "5810", "5820", "5830", "5840", "5850", "5860", "5870", "5880", "5890", "5900", "5910", "5920", "5930", "5940", "5950", "5960", "5970", "5980", "5990", "6000", "6010", "6020", "6030", "6040", "6050", "6060", "6070", "6080", "6090", "6100", "6110", "6120", "6130", "6140", "6150", "6160", "6170", "6180", "6190", "6200", "6210", "6220", "6230", "6240", "6250", "6260", "6270", "6280", "6290", "6300", "6310", "6320", "6330", "6340", "6350", "6360", "6370", "6380", "6390", "6400", "6410", "6420", "6430", "6440", "6450", "6460", "6470", "6480", "6490", "6500"})
		//@Param({"50", "100", "150", "200", "250", "300", "350", "400", "450", "500", "550", "600", "650", "700", "750", "800", "850", "900", "950", "1000", "1050", "1100", "1150", "1200", "1250", "1300", "1350", "1400", "1450", "1500", "1550", "1600", "1650", "1700", "1750", "1800", "1850", "1900", "1950", "2000", "2050", "2100", "2150", "2200", "2250", "2300", "2350", "2400", "2450", "2500", "2550", "2600", "2650", "2700", "2750", "2800", "2850", "2900", "2950", "3000", "3050", "3100", "3150", "3200", "3250", "3300", "3350", "3400", "3450", "3500", "3550", "3600", "3650", "3700", "3750", "3800", "3850", "3900", "3950", "4000", "4050", "4100", "4150", "4200", "4250", "4300", "4350", "4400", "4450", "4500", "4550", "4600", "4650", "4700", "4750", "4800", "4850", "4900", "4950", "5000", "5050", "5100", "5150", "5200", "5250", "5300", "5350", "5400", "5450", "5500", "5550", "5600", "5650", "5700", "5750", "5800", "5850", "5900", "5950", "6000", "6050", "6100", "6150", "6200", "6250", "6300", "6350", "6400", "6450", "6500"})
		@Param({"500", "1000", "1500", "2000", "2500", "3000", "3500", "4000", "4500", "5000", "5500", "6000", "6500", "7000", "7500", "8000", "8500", "9000", "9500", "10000", "10500", "11000", "11500", "12000", "12500", "13000", "13500", "14000", "14500", "15000", "15500", "16000", "16500", "17000", "17500", "18000", "18500", "19000", "19500", "20000", "20500", "21000", "21500", "22000", "22500", "23000", "23500", "24000", "24500", "25000", "25500", "26000", "26500", "27000", "27500", "28000", "28500", "29000", "29500", "30000", "30500", "31000", "31500", "32000", "32500", "33000", "33500", "34000", "34500", "35000", "35500", "36000", "36500", "37000", "37500", "38000", "38500", "39000", "39500", "40000", "40500", "41000", "41500", "42000", "42500", "43000", "43500", "44000", "44500", "45000", "45500", "46000", "46500", "47000", "47500", "48000", "48500", "49000", "49500", "50000", "50500", "51000", "51500", "52000", "52500", "53000", "53500", "54000", "54500", "55000", "55500", "56000", "56500", "57000", "57500", "58000", "58500", "59000", "59500", "60000", "60500", "61000", "61500", "62000", "62500", "63000", "63500", "64000", "64500", "65000"})
		//@Param({"5000", "10000", "15000", "20000", "25000", "30000", "35000", "40000", "45000", "50000", "55000", "60000", "65000", "70000", "75000", "80000", "85000", "90000", "95000", "100000", "105000", "110000", "115000", "120000", "125000", "130000", "135000", "140000", "145000", "150000", "155000", "160000", "165000", "170000", "175000", "180000", "185000", "190000", "195000", "200000", "205000", "210000", "215000", "220000", "225000", "230000", "235000", "240000", "245000", "250000"})
		//@Param({"100000"})

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
		//@Param({"100", "200", "300", "400", "500", "600", "700", "800", "900", "1000", "1100", "1200", "1300", "1400", "1500", "1600", "1700", "1800", "1900", "2000"})
		//@Param({"50", "100", "150", "200", "250", "300", "350", "400", "450", "500", "550", "600", "650", "700", "750", "800", "850", "900", "950", "1000", "1050", "1100", "1150", "1200", "1250", "1300", "1350", "1400", "1450", "1500", "1550", "1600", "1650", "1700", "1750", "1800", "1850", "1900", "1950", "2000"})
		@Param({"50", "100", "150", "200", "250", "300", "350", "400", "450", "500", "550", "600", "650", "700", "750", "800", "850", "900", "950", "1000", "1050", "1100", "1150", "1200", "1250", "1300", "1350", "1400", "1450", "1500", "1550", "1600", "1650", "1700", "1750", "1800", "1850", "1900", "1950", "2000", "2050", "2100", "2150", "2200", "2250", "2300", "2350", "2400", "2450", "2500", "2550", "2600", "2650", "2700", "2750", "2800", "2850", "2900", "2950", "3000"})
		//@Param({"2000"})

		//@Param({"100", "800", "1600", "2400", "3200", "4000"})
		public int n;
		
		@Override
		public int nrEdges(int nrNodes) {
			return (int)(((double)nrNodes) * 2);
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
		//@Param({"8000", "12000", "16000", "20000", "24000", "28000", "32000", "36000", "40000"})
		//@Param({"10", "100", "1000", "5000"})
		//@Param({"5000", "10000", "15000"})
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
		//@Param({"8000", "12000", "16000", "20000", "24000", "28000", "32000", "36000", "40000"})
		@Param({"0", "1", "2", "10", "20"})
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

	//@Benchmark
	@Warmup(iterations = 1)
	@Measurement(iterations = 1)
	@BenchmarkMode(Mode.AverageTime)
	public void testWeakControlClosureViaNTICD(RandomGraphsArbitraryWithNodeSet randomGraphs, Blackhole blackhole) {
		for (int i = 0; i < randomGraphs.getNrOfGraphs(); i++) {
			final Set<Node> result = WeakControlClosure.viaNTICD(randomGraphs.graphs.get(i), randomGraphs.mss.get(i));
			blackhole.consume(result);
		}
	}
	
	//@Benchmark
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
	public void countMyWodSize(RandomGraphsArbitraryNoExitNodes randomGraphs, Size size, Blackhole blackhole) {
		for (int i = 0; i < randomGraphs.getNrOfGraphs(); i++) {
			final Map<Node, Map<Node, Set<Node>>> mywod = NTICDMyWod.compute(randomGraphs.graphs.get(i), edgeFactory, Edge.class);
			blackhole.consume(mywod);
			if (size.size == 0) {
				int sizeMyWod = mywod.values().stream().map(m2 -> m2.values().stream().map(ns -> ns.size()).reduce(0, Integer::sum)).reduce(0, Integer::sum);
				size.size = sizeMyWod;
			}
		}
	}

//	@Benchmark
//	@Warmup(iterations = 1, time = 3)
//	@Measurement(iterations = 1, time = 3)
//	@BenchmarkMode(Mode.AverageTime)
//	public void testNticdMyWodSliceViaMyWod(RandomGraphsArbitraryNoExitNodes randomGraphs, Blackhole blackhole) {
//		for (int i = 0; i < randomGraphs.getNrOfGraphs(); i++) {
//			final Map<Node, Map<Node, Set<Node>>> mywod = NTICDMyWod.compute(randomGraphs.graphs.get(i), edgeFactory, Edge.class);
//			blackhole.consume(mywod);
//		}
//	}
	
	//@Benchmark
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
	
	//@Benchmark
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
	
	//@Benchmark
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
	
	//@Benchmark
	@Warmup(iterations = 1, time = 5)
	@Measurement(iterations = 1, time = 5)
	@BenchmarkMode(Mode.AverageTime)
	public void testRandom(RandomGraphsArbitrary randomGraphs, Blackhole blackhole) {
		for (int i = 0; i < randomGraphs.getNrOfGraphs(); i++) {
			final DirectedGraph<Node, Edge> graph = randomGraphs.graphs.get(i);
			blackhole.consume(NTICDGraphPostdominanceFrontiers.compute(graph, edgeFactory, Edge.class));
		}
	}
	
	//@Benchmark
	@Warmup(iterations = 1, time = 5)
	@Measurement(iterations = 1, time = 5)
	@BenchmarkMode(Mode.AverageTime)
	public void testClassicCDGForRandomWithUniqueExitNode(RandomGraphsWithUniqueExitNode randomGraphs, Blackhole blackhole) {
		for (int i = 0; i < randomGraphs.getNrOfGraphs(); i++) {
			final EntryExitGraph graph = randomGraphs.graphs.get(i);
			blackhole.consume(CDG.build(graph, graph.entry, graph.exit, edgeFactory));
		}
	}
	
	//@Benchmark
	@Warmup(iterations = 1, time = 5)
	@Measurement(iterations = 1, time = 5)
	@BenchmarkMode(Mode.AverageTime)
	public void testNTICDGraphPostdominanceFrontiersForRandomWithUniqueExitNode(RandomGraphsWithUniqueExitNode randomGraphs, Blackhole blackhole) {
		for (int i = 0; i < randomGraphs.getNrOfGraphs(); i++) {
			final EntryExitGraph graph = randomGraphs.graphs.get(i);
			blackhole.consume(NTICDGraphPostdominanceFrontiers.compute(graph, edgeFactory, Edge.class));
		}
	}
	

	//@Benchmark
	@Warmup(iterations = 1, time = 5)
	@Measurement(iterations = 1, time = 5)
	@BenchmarkMode(Mode.AverageTime)
	public void testNTICDGraphPostdominanceFrontiersForEntryExitLadder(EntryExitLadderGraph ladderGraphs, Blackhole blackhole) {
		for (int i = 0; i < ladderGraphs.getNrOfGraphs(); i++) {
			final DirectedGraph<Node, Edge> graph = ladderGraphs.graphs.get(i);
			blackhole.consume(NTICDGraphPostdominanceFrontiers.compute(graph, edgeFactory, Edge.class));
		}
	}
	
	//@Benchmark
	@Warmup(iterations = 1, time = 5)
	@Measurement(iterations = 1, time = 5)
	@BenchmarkMode(Mode.AverageTime)
	public void testNClassicCDGForForEntryExitLadder(EntryExitLadderGraph ladderGraphs, Blackhole blackhole) {
		for (int i = 0; i < ladderGraphs.getNrOfGraphs(); i++) {
			final EntryExitGraph graph = ladderGraphs.graphs.get(i);
			blackhole.consume(CDG.build(graph, graph.entry, graph.exit, edgeFactory));
		}
	}
	
	//@Benchmark
	@Warmup(iterations = 1, time = 5)
	@Measurement(iterations = 1, time = 5)
	@BenchmarkMode(Mode.AverageTime)
	public void testSinkPostdominanceFrontiersForEntryExitLadder(EntryExitLadderGraph ladderGraphs, Blackhole blackhole) {
		for (int i = 0; i < ladderGraphs.getNrOfGraphs(); i++) {
			final DirectedGraph<Node, Edge> graph = ladderGraphs.graphs.get(i);
			blackhole.consume(SinkpathPostDominators.compute(graph));
		}
	}
	
	//@Benchmark
	@Warmup(iterations = 1, time = 5)
	@Measurement(iterations = 1, time = 5)
	@BenchmarkMode(Mode.AverageTime)
	public void testClassicPostdominanceFrontiersGForForEntryExitLadder(EntryExitLadderGraph ladderGraphs, Blackhole blackhole) {
		for (int i = 0; i < ladderGraphs.getNrOfGraphs(); i++) {
			final EntryExitGraph graph = ladderGraphs.graphs.get(i);
			blackhole.consume(ClassicPostdominance.classicPostdominance(graph, graph.entry, graph.exit));
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
		//new MyBenchmark().testClassicCDGForRandomWithUniqueExitNode(                      randomGraphs, blackhole);
		//new MyBenchmark().testNTICDGraphPostdominanceFrontiersForRandomWithUniqueExitNode(randomGraphs, blackhole);
	}
	
	//public static void mainPrintParam(String[] args) {
	public static void main(String[] args) {
		final int nr     = 60;
		final int stride = 50; 
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
		Options opt = new OptionsBuilder()
			.include(MyBenchmark.class.getSimpleName())
			.forks(1)
			.build();
		new Runner(opt).run();
	}
}
