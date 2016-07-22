package tests;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import edu.kit.joana.graph.dominators.AbstractCFG;
import edu.kit.joana.graph.dominators.CustomCFG;
import edu.kit.joana.graph.dominators.InterprocDominators2;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ifc.sdg.util.graph.io.dot.MiscGraph2Dot;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class DomExperiment {

	private static <V, E> DirectedGraph<V, DefaultEdge> makeDomGraph(final AbstractCFG<V, E> icfg) {
		final InterprocDominators2<V, E> dom = new InterprocDominators2<V, E>(icfg);
		dom.runWorklist();
		final DirectedGraph<V, DefaultEdge> domGraph = new DefaultDirectedGraph<V, DefaultEdge>(DefaultEdge.class);
		for (final V v : icfg.vertexSet()) {
			domGraph.addVertex(v);
		}
		for (final V v : icfg.vertexSet()) {
			for (final V vDom : dom.idoms(v)) {
				domGraph.addEdge(vDom, v);
			}
		}
		return domGraph;
	}

	private static CustomCFG<Integer, DefaultEdge> example1() {
		final CustomCFG<Integer, DefaultEdge> icfg = new CustomCFG<Integer, DefaultEdge>(DefaultEdge.class);
		icfg.addNormalEdge(1, 2);
		icfg.addNormalEdge(1, 10);
		icfg.addNormalEdge(8, 9);
		icfg.addNormalEdge(12, 9);
		icfg.addNormalEdge(3, 4);
		icfg.addNormalEdge(6, 7);
		icfg.addCall(2, 3, 4, 5);
		icfg.addCall(11, 3, 4, 12);
		icfg.addCall(5, 6, 7, 8);
		icfg.addCall(10, 6, 7, 11);
		icfg.setRoot(1);
		return icfg;
	}

	private static CustomCFG<Integer, DefaultEdge> example2() {
		final CustomCFG<Integer, DefaultEdge> icfg = new CustomCFG<Integer, DefaultEdge>(DefaultEdge.class);
		icfg.addNormalEdge(1, 2);
		icfg.addNormalEdge(2, 3);
		icfg.addNormalEdge(3, 4);
		icfg.addNormalEdge(3, 5);
		icfg.addNormalEdge(4, 2);
		icfg.addNormalEdge(4, 10);
		icfg.addNormalEdge(5, 6);
		icfg.addCall(6, 8, 9, 7);
		icfg.addNormalEdge(7, 11);
		icfg.addNormalEdge(8, 9);
		icfg.addNormalEdge(10, 12);
		icfg.addNormalEdge(11, 12);
		icfg.setRoot(1);
		return icfg;
	}

	private static CustomCFG<Integer, DefaultEdge> example3() {
		final CustomCFG<Integer, DefaultEdge> icfg = new CustomCFG<Integer, DefaultEdge>(DefaultEdge.class);
		icfg.addNormalEdge(1, 2);
		icfg.addCall(2, 3, 12, 9);
		icfg.addNormalEdge(3, 4);
		icfg.addNormalEdge(4, 5);
		icfg.addNormalEdge(4, 12);
		icfg.addNormalEdge(5, 6);
		icfg.addNormalEdge(6, 7);
		icfg.addNormalEdge(7, 8);
		icfg.addNormalEdge(8, 11);
		icfg.addNormalEdge(9, 10);
		icfg.addNormalEdge(10, 11);
		icfg.addNormalEdge(11, 13);
		icfg.setRoot(1);
		return icfg;
	}

	private static <V, E> void runExample(final AbstractCFG<V, E> example, final DOTExporter<V, E> exporterCFG,
			final String cfFileName, final DOTExporter<V, DefaultEdge> exporterDom, final String dgFileName)
					throws FileNotFoundException {
		final DirectedGraph<V, DefaultEdge> domGraph = makeDomGraph(example);
		MiscGraph2Dot.export(example.getUnderlyingGraph(), exporterCFG, cfFileName);
		MiscGraph2Dot.export(domGraph, exporterDom, dgFileName);
	}

	public static void main(final String[] args) throws FileNotFoundException {
		runExample(example1(), MiscGraph2Dot.standardExporter(), "controlFlow1.dot", MiscGraph2Dot.standardExporter(), "domGraph1.dot");
		runExample(example2(), MiscGraph2Dot.standardExporter(), "controlFlow2.dot", MiscGraph2Dot.standardExporter(), "domGraph2.dot");
		runExample(example3(), MiscGraph2Dot.standardExporter(), "controlFlow3.dot", MiscGraph2Dot.standardExporter(), "domGraph3.dot");
	}
}
