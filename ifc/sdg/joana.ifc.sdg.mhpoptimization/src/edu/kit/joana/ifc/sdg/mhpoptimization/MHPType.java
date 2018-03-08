/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.mhpoptimization;

import java.util.function.Function;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.SimpleMHPAnalysis;

public enum MHPType {
	NONE(sdg -> null),
	SIMPLE(sdg -> SimpleMHPAnalysis.analyze(sdg)),
	PRECISE_UNSAFE(sdg -> PreciseMHPAnalysis.analyzeUNSAFE(sdg)),
	PRECISE(sdg -> PreciseMHPAnalysis.analyze(sdg));
	
	private final Function<SDG, MHPAnalysis> mhpAnalysisConstructor;
	
	private MHPType(Function<SDG, MHPAnalysis> mhpAnalysisConstructor) {
		this.mhpAnalysisConstructor = mhpAnalysisConstructor;
	}
	
	public Function<SDG, MHPAnalysis> getMhpAnalysisConstructor() {
		return mhpAnalysisConstructor;
	}
}
