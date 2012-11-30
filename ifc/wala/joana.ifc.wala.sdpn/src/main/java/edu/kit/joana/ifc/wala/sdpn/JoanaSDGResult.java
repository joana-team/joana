/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.wala.sdpn;

import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;

public class JoanaSDGResult {
	public final edu.kit.joana.ifc.sdg.graph.SDG sdg;
	public final CallGraph cg;
	public final PointerAnalysis pts;
	public final MHPAnalysis mhp;

	public JoanaSDGResult(final edu.kit.joana.ifc.sdg.graph.SDG sdg, final CallGraph cg, final PointerAnalysis pts,
			final MHPAnalysis mhp) {
		this.sdg = sdg;
		this.cg = cg;
		this.pts = pts;
		this.mhp = mhp;
	}

}
