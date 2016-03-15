package edu.kit.joana.ifc.orlsod;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.BinaryViolation;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;

public class PathBasedORLSODChecker<L> extends OptORLSODChecker<L> {

	private DirectedGraph<SDGNode, DefaultEdge> depGraph;

	public PathBasedORLSODChecker(SDG sdg, IStaticLattice<L> secLattice, Map<SDGNode, L> srcAnn, ProbInfComputer probInf) {
		super(sdg, secLattice, srcAnn, probInf);
	}

	@Override
	public Collection<? extends IViolation<SecurityNode>> checkIFlow() throws NotInLatticeException {
		this.depGraph = new DefaultDirectedGraph<SDGNode, DefaultEdge>(DefaultEdge.class);
		for (SDGNode n : sdg.vertexSet()) {
			for (SDGNode inflN : computeBackwardDeps(n)) {
				depGraph.addVertex(inflN);
				depGraph.addVertex(n);
				depGraph.addEdge(inflN, n);
			}
		}
		List<BinaryViolation<SecurityNode, L>> ret = new LinkedList<BinaryViolation<SecurityNode, L>>();
		for (Map.Entry<SDGNode, L> userEntry1 : userAnn.entrySet()) {
			for (Map.Entry<SDGNode, L> userEntry2 : userAnn.entrySet()) {
				if (LatticeUtil.isLeq(this.secLattice, userEntry1.getValue(), userEntry2.getValue())) continue;
				List<DefaultEdge> path = DijkstraShortestPath.findPathBetween(depGraph, userEntry1.getKey(), userEntry2.getKey());
				if (path != null) {
					System.out.println(path);
					ret.add(new BinaryViolation<SecurityNode, L>(new SecurityNode(userEntry2.getKey()), new SecurityNode(userEntry1.getKey()), userEntry2.getValue()));
				} else {
					System.out.println(String.format("%s cannot influence %s.", userEntry1.getKey(), userEntry2.getKey()));
				}
			}
		}
		return ret;
	}
}
