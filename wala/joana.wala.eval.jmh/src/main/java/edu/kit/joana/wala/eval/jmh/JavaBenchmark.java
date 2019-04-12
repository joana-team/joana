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

	public static final EdgeFactory<PDGNode, PDGEdge> edgeFactory = new EdgeFactory<PDGNode,PDGEdge>() {
		public PDGEdge createEdge(PDGNode from, PDGNode to) {
			return new PDGEdge(from, to, PDGEdge.Kind.CONTROL_DEP);
		};
	};

	
	public abstract static class Graphs<V extends IntegerIdentifiable, E extends KnowsVertices<V>> {
		private final boolean dumpingEnabled = true;
		public ArrayList<DirectedGraph<V,E>> graphs;
		
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
