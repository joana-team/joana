/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.violations;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.declass.MinimalCut;
import edu.kit.joana.ifc.sdg.core.violations.Violation;
import edu.kit.joana.ifc.sdg.core.violations.paths.ConcurrentViolationChop;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPath;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPathes;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;

public class MinimalCutChopper implements IChopper {

	@Override
	public Violation addChop(IProject p, Violation violation, SDG g, IStaticLattice<String> l) {
		SDGNodeTuple range = new SDGNodeTuple(violation.getSource(), violation.getSink());
		ConcurrentViolationChop.getInstance().initChopper(g);
		Collection<SecurityNode> subNGraph = ConcurrentViolationChop.getInstance().chop(violation.getSink(), violation.getSource());
		Collection<SDGNode> subGraph = new LinkedList<SDGNode>();
		for (SecurityNode n : subNGraph) {
			subGraph.add(n);
		}
		Collection<SDGEdge> cut = MinimalCut.findMinimalCut(g, subGraph, range);
		ViolationPathes pathes = new ViolationPathes();
		for (SDGEdge e : cut) {
			ViolationPath path = new ViolationPath();
			SecurityNode source = (SecurityNode) e.getSource();
			SecurityNode target = (SecurityNode) e.getTarget();
			path.add(source);
			if (source != target) {
				path.add(target);
			}
			pathes.add(path);
		}
		Violation newv = Violation.createViolation(violation.getSink(), violation.getSource(), pathes, violation.getSink().getRequired());

		return newv;
	}

	@Override
	public void initializeGUI(Action action) {
		action.setText("Find MinimalCut");
		action.setImageDescriptor(NJSecPlugin.singleton().getImageRegistry().getDescriptor("pdg"));
	}

	@Override
	public String getName() {
		return "MinimalCut";
	}

}
