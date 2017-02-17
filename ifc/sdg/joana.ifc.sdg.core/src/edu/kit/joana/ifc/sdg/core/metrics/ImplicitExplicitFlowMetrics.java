/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.metrics;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.ContextSensitiveThreadChopper;


public class ImplicitExplicitFlowMetrics implements IMetrics {
	private final ComputeMetrics metrics;

	public ImplicitExplicitFlowMetrics() {
		metrics = new ComputeMetrics();
	}

	public Collection<ClassifiedViolation> computeMetrics(SDG g, Collection<ClassifiedViolation> vios) {
		metrics.setGraph(g);

		for (ClassifiedViolation v : vios) {
			int filters = metrics.slice(v);
			IMetrics.Rating rat = determineRating(filters);
			v.addClassification("Implicit Flow", "Filtered by at least "+filters+" predicates.", filters, rat);
//			System.out.println(v);
//			System.out.println("Filtered by at least "+filters+" predicates.");
		}

		return vios;
	}

	private IMetrics.Rating determineRating(int filters) {
		if (filters == 0) return IMetrics.Rating.DANGEROUS;
		else if (filters < 100) return IMetrics.Rating.MOSTLY_HARMLESS;
		else return IMetrics.Rating.HARMLESS;
	}

	private static class ComputeMetrics {
		private SDG g;
		private ContextSensitiveThreadChopper chopper;
		private final Set<SDGEdge.Kind> omittedEdges;
		private final Set<SDGEdge.Kind> stepEdges;

		private ComputeMetrics() {
			omittedEdges = new HashSet<SDGEdge.Kind>();
			omittedEdges.add(SDGEdge.Kind.SUMMARY);
			omittedEdges.add(SDGEdge.Kind.SUMMARY_DATA);
			omittedEdges.add(SDGEdge.Kind.SUMMARY_NO_ALIAS);

			stepEdges = new HashSet<SDGEdge.Kind>();
//			stepEdges.add(SDGEdge.Kind.CONTROL_DEP_UNCOND);
			stepEdges.add(SDGEdge.Kind.CONTROL_DEP_COND);
//			stepEdges.add(SDGEdge.Kind.CONTROL_DEP_EXPR);
//			stepEdges.add(SDGEdge.Kind.CONTROL_DEP_CALL);
			stepEdges.add(SDGEdge.Kind.JUMP_DEP);
			stepEdges.add(SDGEdge.Kind.CALL);
			stepEdges.add(SDGEdge.Kind.FORK);
			stepEdges.add(SDGEdge.Kind.JOIN);
		}

		private void setGraph(SDG g) {
			this.g = g;

			if (chopper == null) {
				chopper = new ContextSensitiveThreadChopper(g);
			} else {
				chopper.setGraph(g);
			}
		}

		private int slice(IIllegalFlow<SecurityNode> vio) {
			Collection<SDGNode> chop = chopper.chop(vio.getSource(), vio.getSink());

			int step = 0;
			StepResult sr = null;
			Collection<SDGNode> stepCriteria = Collections.singleton((SDGNode) vio.getSink());
			boolean found = false;

			do {
				sr = slice(stepCriteria, chop);

				if (sr.stepSlice.contains(vio.getSource())) {
					found = true;

				} else if (sr.nextStep.contains(vio.getSource())) {
					found = true;
					step++;

				} else {
					stepCriteria = sr.nextStep;
					step++;
				}

			} while (!found && sr.nextStep());

			return step;
		}

		private StepResult slice(Collection<SDGNode> criteria, Collection<SDGNode> subgraph) {
	    	HashSet<SDGNode> slice = new HashSet<SDGNode>();
	    	HashSet<SDGNode> beyond = new HashSet<SDGNode>();
	    	LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
	    	worklist.addAll(criteria);

	    	for (SDGNode node : criteria) {
	    		slice.add(node);
	    	}

	    	while (!worklist.isEmpty()) {
	    		SDGNode w = worklist.poll();

	    		for (SDGEdge e : g.incomingEdgesOf(w)) {
	    			if (!e.getKind().isSDGEdge() || omittedEdges.contains(e.getKind()))
	    				continue;

	    			SDGNode v = e.getSource();

	    			if (!subgraph.contains(v)) continue;

	    			if (stepEdges.contains(e.getKind())) {
	    				beyond.add(v);

	    			} else if (slice.add(v)) {
	    				worklist.addFirst(v);
	    			}
	    		}
	    	}

	    	return new StepResult(slice, beyond);
	    }
	}

	private static class StepResult {
		private final Set<SDGNode> stepSlice;
		private final Set<SDGNode> nextStep;

		StepResult(Set<SDGNode> stepSlice, Set<SDGNode> nextStep) {
			this.stepSlice = stepSlice;
			this.nextStep = nextStep;
		}

		boolean nextStep() {
			return !nextStep.isEmpty();
		}

		public String toString() {
			return "("+stepSlice+", "+nextStep+")";
		}
	}
}
