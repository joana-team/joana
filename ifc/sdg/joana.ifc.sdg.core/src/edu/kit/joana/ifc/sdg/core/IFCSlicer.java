/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;


/**
 * @author hammer
 *
 */
public class IFCSlicer {

	IStaticLattice<String> lattice;
	SDG graph;

	public IFCSlicer(IStaticLattice<String> l, SDG g) {
		lattice = l;
		graph = g;
	}

	private boolean sliceSizeOk(SecurityNode startNode, Map<SecurityNode, String> sLevel) {
		Collection<SDGNode> slice = new SummarySlicerBackward(graph).slice(
				Collections.singleton((SDGNode) startNode));
		Set<SecurityNode> keySet = sLevel.keySet();
		
		return slice.size() == keySet.size() && slice.containsAll(keySet);
	}
	
	public List<ClassifiedViolation> checkIFC(SecurityNode startNode) {
		List<ClassifiedViolation> violations = new ArrayList<ClassifiedViolation>();
		LinkedList<SecurityNode> workList = new LinkedList<SecurityNode>();
		Map<SecurityNode, String> sLevel = new HashMap<SecurityNode, String>();
		workList.add(startNode);
		sLevel.put(startNode, startNode.getRequired());
		checkIFC1(workList, sLevel);

		assert sliceSizeOk(startNode, sLevel);
		
		// check that P(x) <= S(x) for all nodes in slice
		for (SecurityNode toCheck : sLevel.keySet()) {
			String securityLabel = sLevel.get(toCheck);
			if (toCheck.getProvided() != SecurityNode.UNDEFINED && !leq(toCheck, securityLabel)) {
				ClassifiedViolation v = ClassifiedViolation.createViolation(toCheck, startNode, toCheck.getRequired());
				violations.add(v);
			}
		}
		return violations;
	}

	private boolean leq(SecurityNode toCheck, String securityLabel) {
		String p = toCheck.getProvided();
		return lattice.greatestLowerBound(securityLabel, p).equals(p);
	}

	void checkIFC1(LinkedList<SecurityNode> workList, Map<SecurityNode, String> sLevel) {
		LinkedList<SecurityNode> workListDown = new LinkedList<SecurityNode>();
		while (!workList.isEmpty()) {
			SecurityNode current = workList.remove();
			String securityLabel = sLevel.get(current);
			if (current.isInformationSink()) {
				securityLabel = lattice.greatestLowerBound(securityLabel, current.getRequired());
			}
			for (SDGEdge e : graph.incomingEdgesOf(current)) {
				if (!e.getKind().isSDGEdge() || e.getKind().isThreadEdge())
					continue;
				SecurityNode pred = (SecurityNode) e.getSource();
				String oldSec = sLevel.get(pred);
				String newSec;
				if (e.getKind() == Kind.SUMMARY && e.getLabel() != null)
					newSec = computeLevel(current, securityLabel, oldSec, e.getLabel());
				else
					newSec = computeLevel(current, securityLabel, oldSec);
				if (!newSec.equals(oldSec)) {
					sLevel.put(pred, newSec);
					if (e.getKind() == Kind.PARAMETER_OUT) {
						workListDown.add(pred);
					} else {
						workList.add(pred);
					}
				}
			}
		}
		checkIFC2(workListDown, sLevel);
	}

	/**
	 * Computes the security level of <i>current</i> from its old <i>securityLabel</i>
	 * and the security level <i>oldSec</i> of one of its predecessors.
	 *
	 * @param current SecurityNode for which the security level is to be computed
	 * @param securityLabel
	 * @param oldSec
	 * @return
	 */
	String computeLevel(SecurityNode current, String securityLabel, String oldSec) {
		String newSec;
		if (current.isDeclassification()) {
			newSec = lattice.greatestLowerBound(
					oldSec == null ? lattice.getTop() : oldSec, current.getRequired());
		} else {
			newSec = lattice.greatestLowerBound(
					oldSec == null ? lattice.getTop() : oldSec, securityLabel);
		}
		return newSec;
	}

	/**
	 * Computes the security level of <i>current</i> from its old <i>securityLabel</i>
	 * and the security level <i>oldSec</i> of one of its predecessors.
	 *
	 * @param current SecurityNode for which the security level is to be computed
	 * @param securityLabel
	 * @param oldSec
	 * @return
	 */
	String computeLevel(SecurityNode current, String securityLabel, String oldSec, String sumLabel) {
		String newSec;
		if (current.isDeclassification()) {
			newSec = lattice.greatestLowerBound(
					oldSec == null ? lattice.getTop() : oldSec,
							sumLabel == null ? current.getRequired() : sumLabel);
		} else {
			newSec = lattice.greatestLowerBound(
					oldSec == null ? lattice.getTop() : oldSec,
							sumLabel == null ? securityLabel : sumLabel);
		}
		return newSec;
	}

	void checkIFC2(LinkedList<SecurityNode> workList, Map<SecurityNode, String> sLevel) {
		while (!workList.isEmpty()) {
			SecurityNode current = workList.remove();
			String securityLabel = sLevel.get(current);
			for (SDGEdge e : graph.incomingEdgesOf(current)) {
				if (!e.getKind().isSDGEdge())
					continue;
				switch (e.getKind()) {
				case CALL:
				case PARAMETER_IN:
					continue;
				default:
					SecurityNode pred = (SecurityNode) e.getSource();
					String oldSec = sLevel.get(pred);
					String newSec;
					if (e.getKind() == Kind.SUMMARY && e.getLabel() != null)
						newSec = computeLevel(current, securityLabel, oldSec, e.getLabel());
					else
						newSec = computeLevel(current, securityLabel, oldSec);
					if (!newSec.equals(oldSec)) {
						sLevel.put(pred, newSec);
						workList.add(pred);
					}
				}
			}
		}
	}
}
