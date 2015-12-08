package edu.kit.joana.graph.dominators.slca;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import tests.DomExperiment;


public class GraphTest {

	private static DirectedGraph<Integer, DefaultEdge> randomGraph(Random r, int numNodes, int numEdges) {
		DirectedGraph<Integer, DefaultEdge> ret = new DefaultDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);
		for (int i = 0; i < numNodes; i++) {
			ret.addVertex(i);
		}
		for (int j = 0; j < numEdges; j++) {
			int n1 = 0;
			int n2 = 0;
			boolean hasCycle = true;
			do {
				n1 = r.nextInt(numNodes);
				while (n2 == 0) n2 = r.nextInt(numNodes);
				ret.addEdge(n1, n2);
				hasCycle = new CycleDetector<Integer, DefaultEdge>(ret).detectCycles();
				ret.removeEdge(n1, n2);
			} while (hasCycle);
			ret.addEdge(n1, n2);
			System.out.println(String.format("(%d,%d)", n1, n2));
		}
		List<Integer> toRemove = new LinkedList<Integer>();
		for (int v : ret.vertexSet()) {
			if (v != 0 && ret.inDegreeOf(v) == 0) {
				ret.addEdge(0, v);
			}
		}
		ret.removeAllVertices(toRemove);
		return ret;
	}

	public static void main(String[] args) throws FileNotFoundException {
		//DirectedGraph<String, DefaultEdge> dag = new DirectedAcyclicGraph<String, DefaultEdge>(DefaultEdge.class);
		DirectedGraph<Integer, DefaultEdge> g = randomGraph(new Random(), 10, 15);
		DomExperiment.export(g, DomExperiment.standardExporter(), "random.dot");
//		dag.addVertex("0");
//		dag.addVertex("1");
//		dag.addVertex("2");
//		dag.addVertex("3");
//		dag.addVertex("4");
//		dag.addVertex("5");
//		dag.addEdge("0", "1");
//		dag.addEdge("1", "2");
//		dag.addEdge("1", "3");
//		dag.addEdge("2", "4");
		//dag.addEdge("2", "5");
		//dag.addEdge("3", "4");
//		dag.addEdge("3", "5");
		DFSIntervalOrder<Integer, DefaultEdge> rpo = new DFSIntervalOrder<Integer, DefaultEdge>(g);
		System.out.println(rpo.listVertices());
		DominatorComputation<Integer, DefaultEdge> domComp = new DominatorComputation<Integer, DefaultEdge>(g, 0);
		DomExperiment.export(domComp.getDominatorTree(), DomExperiment.standardExporter(), "random-domtree.dot");
		SLCAComputation<Integer, DefaultEdge> slcaComp = new SLCAComputation<Integer, DefaultEdge>(g, 0);
		for (int v1 : g.vertexSet()) {
			for (int v2 : g.vertexSet()) {
				if (v1 != v2) {
					System.out.println(String.format("slca[%s,%s] = %s", v1, v2, slcaComp.slca(v1, v2)));
				}
			}
		}
	}
}
