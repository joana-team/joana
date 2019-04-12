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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.graph.EdgeReversedGraph;
import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.AuxCounters.Type;
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

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.sdg.SDGBuildPreparation;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;
import edu.kit.joana.util.io.IOFactory;
import edu.kit.joana.wala.core.DependenceGraph;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ControlDependenceVariant;
import edu.kit.joana.wala.core.graphs.EfficientDominators;
import edu.kit.joana.wala.core.graphs.NTICDGraphPostdominanceFrontiers;
import edu.kit.joana.wala.core.graphs.SinkdomControlSlices;
import edu.kit.joana.wala.core.graphs.SinkpathPostDominators;
import edu.kit.joana.wala.core.graphs.EfficientDominators.DomTree;
import edu.kit.joana.wala.core.graphs.SinkpathPostDominators.ISinkdomEdge;
import edu.kit.joana.wala.core.graphs.FCACD;
import edu.kit.joana.wala.core.graphs.NTICDControlSlices;
import edu.kit.joana.wala.util.WriteGraphToDot;

@Fork(value = 1, jvmArgsAppend = "-Xss128m")
public class JavaBenchmark {
	
	public static final String JOANA_API_TEST_DATA_CLASSPATH = "../../api/joana.api.testdata/bin";
	public static final String ANNOTATIONS_PASSON_CLASSPATH = "../../api/joana.api.annotations.passon/bin";
	
	private static final Stubs STUBS = Stubs.JRE_15;
	
	private static void setDefaults(SDGConfig config) {
		config.setParallel(false);
		config.setComputeSummaryEdges(false);
	}

	
	public static final SDGConfig nticd_isinkdom = new SDGConfig(
			JOANA_API_TEST_DATA_CLASSPATH,
			null,
			STUBS
		); {
			setDefaults(nticd_isinkdom);
			nticd_isinkdom.setControlDependenceVariant(ControlDependenceVariant.NTICD_ISINKDOM);
	}

	
	/*
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
	        final CDG cdg = new CDG(cfg, exit, edgeFactory);

	        cdg.build();

	        return cdg;
	    }

	    private final DirectedGraph<Node, Edge> cfg;
	    private final Node exit;

	    private CDG(final DirectedGraph<Node, Edge> cfg, Node exit, EdgeFactory<Node, Edge> edgeFactory) {
	        super(edgeFactory, () -> new LinkedHashMap<>(cfg.vertexSet().size()), Edge.class);
	        this.cfg = cfg;
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
	*/
	
	public static class ClassicPostdominance {

		public static DomTree<PDGNode> classicPostdominance(DirectedGraph<PDGNode, PDGEdge> cfg, PDGNode entry, PDGNode exit) {
			final DirectedGraph<PDGNode, PDGEdge> reversedCfg = new EdgeReversedGraph<PDGNode, PDGEdge>(cfg);
			final EfficientDominators<PDGNode, PDGEdge> dom = EfficientDominators.compute(reversedCfg, exit);

			return dom.getDominationTree();
		}
	}
	
	public static class WeakControlClosure {
		public static Set<PDGNode> viaNTICD(DirectedGraph<PDGNode, PDGEdge> graph, Set<PDGNode> ms) {
			final Set<PDGNode> result = NTICDControlSlices.wcc(graph, ms, PDGEdge.class, edgeFactory);
			return result;
		}
		
		public static Set<PDGNode> viaISINKDOM(DirectedGraph<PDGNode, PDGEdge> graph, Set<PDGNode> ms) {
			final Set<PDGNode> result = SinkdomControlSlices.wcc(graph, ms, PDGEdge.class, edgeFactory);
			return result;
		}
		
		public static Set<PDGNode> viaFCACD(DirectedGraph<PDGNode, PDGEdge> graph, Set<PDGNode> ms) {
			final Set<PDGNode> result = FCACD.wcc(graph, ms);
			return result;
		}
	}
	
	public static class NticdMyWod {
		public static Set<PDGNode> viaMYWOD(DirectedGraph<PDGNode, PDGEdge> graph, Set<PDGNode> ms) {
			final Set<PDGNode> result = NTICDControlSlices.nticdMyWod(graph, ms, PDGEdge.class, edgeFactory);
			return result;
		}
	}

	/*
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
	*/
	
	/*
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
			//System.out.print(e + "  ");
			
		}
		//System.out.println();
	}
	*/
	
	public static final EdgeFactory<PDGNode, PDGEdge> edgeFactory = new EdgeFactory<PDGNode,PDGEdge>() {
		public PDGEdge createEdge(PDGNode from, PDGNode to) {
			return new PDGEdge(from, to, PDGEdge.Kind.CONTROL_DEP);
		};
	};

	
	public abstract static class Graphs<V extends IntegerIdentifiable, E extends KnowsVertices<V>> {
		private final boolean dumpingEnabled = true;
		public ArrayList<DirectedGraph<V,E>> graphs;
		
		/*
		public final VertexFactory<PDGNode> newVertexFactory() {
			return new VertexFactory<JavaBenchmark.Node>() {
				private int id = 0;
				@Override
				public Node createVertex() {
					return new Node(id++);
				}
			};
		}
		*/
		
		public void dumpGraph(int n, int i, DirectedGraph<V,E> graph) {
			if (!dumpingEnabled) return;
			final String cfgFileName = WriteGraphToDot.sanitizeFileName(this.getClass().getSimpleName()+"-" + graph.getClass().getName() + "-" + n + "-" + i +"-cfg.dot");
			try {
				WriteGraphToDot.write(graph, cfgFileName, e -> true, v -> Integer.toString(v.getId()));
			} catch (FileNotFoundException e) {
			}
			final DirectedGraph<SinkpathPostDominators.Node<V>, ISinkdomEdge<SinkpathPostDominators.Node<V>>> isinkdom = SinkpathPostDominators.compute(graph).getResult();
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

	@State(Scope.Benchmark)
	public static class JavaGraphs extends Graphs<PDGNode, PDGEdge> {
		final String[] classes = new String[] { "JLex.Main" };
		@Setup(Level.Trial)
		public void doSetup() throws ClassHierarchyException, UnsoundGraphException, CancelException, IOException {
			final SDGConfig config = nticd_isinkdom;
			this.graphs = new ArrayList<>();
			int i = 0;

			
			for (String className : classes) {
				
				final String classPath = JOANA_API_TEST_DATA_CLASSPATH + File.pathSeparator + ANNOTATIONS_PASSON_CLASSPATH;
				config.setClassPath(classPath);
				JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
				config.setEntryMethod(mainMethod.toBCString());

				
				final PrintStream out = IOFactory.createUTF8PrintStream(new ByteArrayOutputStream());
				final IProgressMonitor monitor = NullProgressMonitor.INSTANCE;
				
				final com.ibm.wala.util.collections.Pair<SDG, SDGBuilder> p =
						SDGBuildPreparation.computeAndKeepBuilder(out, SDGProgram.makeBuildPreparationConfig(config), monitor);
				final SDGBuilder builder = p.snd;
				
				for (PDG pdg : builder.getAllPDGs()) {
					final DependenceGraph cfg = pdg.createCfgWithoutParams();
					final List<PDGNode> toRemove = new LinkedList<>();
					for (PDGNode n : pdg.vertexSet()) {
						if (n.getKind() == PDGNode.Kind.ENTRY && !pdg.entry.equals(n)) toRemove.add(n);
					}
					for (PDGNode n : toRemove) {
						if (!cfg.outgoingEdgesOf(n).isEmpty()) throw new AssertionError();
						cfg.removeNode(n);
					}
					
					cfg.addEdge(pdg.entry, pdg.exit, new PDGEdge(pdg.entry, pdg.exit, PDGEdge.Kind.CONTROL_FLOW));
					this.graphs.add(cfg);
					dumpGraph(cfg.vertexSet().size(), i++, cfg);
				}
			}
		}

	}
	

	@Benchmark
	@Warmup(iterations = 1)
	@Measurement(iterations = 1)
	@BenchmarkMode(Mode.AverageTime)
	public void testWeakControlClosureViaNTICD(JavaGraphs javaGraphs, Blackhole blackhole) {
		for (DirectedGraph<PDGNode, PDGEdge> cfg : javaGraphs.graphs) {
			final NTICDGraphPostdominanceFrontiers<PDGNode, PDGEdge> result = NTICDGraphPostdominanceFrontiers.compute(cfg, edgeFactory, PDGEdge.class);
			blackhole.consume(result);
		}
	}
	
	

	
	public static void mainManual(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
			.include(JavaBenchmark.class.getSimpleName())
			.forks(1)
			.build();
		new Runner(opt).run();
	}
}
