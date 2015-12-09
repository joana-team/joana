package edu.kit.joana.ifc.orlsod;

import java.util.List;
import java.util.Map;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;

public class PathBasedORLSODChecker<L> extends OptORLSODChecker<L> {

	private DirectedGraph<SDGNode, DefaultEdge> depGraph;

	public PathBasedORLSODChecker(SDG sdg, IStaticLattice<L> secLattice, Map<SDGNode, L> srcAnn, ProbInfComputer probInf) {
		super(sdg, secLattice, srcAnn, probInf);
	}

	@Override
	public int check() {
		this.depGraph = new DefaultDirectedGraph<SDGNode, DefaultEdge>(DefaultEdge.class);
		for (SDGNode n : sdg.vertexSet()) {
			for (SDGNode inflN : computeBackwardDeps(n)) {
				depGraph.addVertex(inflN);
				depGraph.addVertex(n);
				depGraph.addEdge(inflN, n);
			}
		}
		int noVios = 0;
		for (Map.Entry<SDGNode, L> userEntry1 : userAnn.entrySet()) {
			for (Map.Entry<SDGNode, L> userEntry2 : userAnn.entrySet()) {
				if (userEntry2.equals(userEntry1)) continue;
				List<DefaultEdge> path = DijkstraShortestPath.findPathBetween(depGraph, userEntry1.getKey(), userEntry2.getKey());
				if (path != null) {
					System.out.println(path);
					noVios++;
				} else {
					System.out.println(String.format("%s cannot influence %s.", userEntry1.getKey(), userEntry2.getKey()));
				}
			}
		}
		return noVios;
	}
}
