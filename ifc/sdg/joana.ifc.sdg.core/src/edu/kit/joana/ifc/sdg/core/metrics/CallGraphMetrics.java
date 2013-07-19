/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.metrics;

import java.util.Collection;
import java.util.HashMap;

import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicer;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerForward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.CallGraphBuilder;


public class CallGraphMetrics implements IMetrics {
	private static HashMap<SDG, CFG> cache = new HashMap<SDG, CFG>();

	private SummarySlicer back;
	private SummarySlicer forw;
	private CFG call;

	public Collection<ClassifiedViolation> computeMetrics(SDG g, Collection<ClassifiedViolation> vios) {
		call = cache.get(g);

		if (call == null) {
			call = CallGraphBuilder.buildEntryGraph(g);
			cache.put(g, call);
		}

		back = new SummarySlicerBackward(g);
		forw = new SummarySlicerForward(g);

		for (ClassifiedViolation v : vios) {
			foo(v);
		}

		return vios;
	}

	private void foo(ClassifiedViolation v) {
		SDGNode source = v.getSource();
		SDGNode sink = v.getSink();

		if (source.getProc() == sink.getProc()) {
			// 1. Fall
			v.addClassification("Call Graph", "Leak in the same procedure", 3, IMetrics.Rating.HARMLESS);

		} else {
			SDGNode sourceEntry = call.getEntry(source);
			SDGNode sinkEntry = call.getEntry(sink);

			if (back.slice(sinkEntry).contains(sourceEntry)) {
				// 2. Fall
				v.addClassification("Call Graph", "Leak in called procedure", 2, IMetrics.Rating.MOSTLY_HARMLESS);

			} else if (forw.slice(sinkEntry).contains(sourceEntry)) {
				// 3. Fall
				v.addClassification("Call Graph", "Leak in calling procedure", 1, IMetrics.Rating.MOSTLY_HARMLESS);

			} else {
				// 4. Fall
				v.addClassification("Call Graph", "Leak in unrelated procedure", 0, IMetrics.Rating.DANGEROUS);
			}
		}
	}
}
