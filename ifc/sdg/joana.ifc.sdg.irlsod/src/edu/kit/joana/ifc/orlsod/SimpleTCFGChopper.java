package edu.kit.joana.ifc.orlsod;

import java.util.Collection;
import java.util.Collections;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.IntersectionChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGForward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;

/**
 * This is a simple chopper for threaded interprocedural control-flow graphs which employs an
 * algorithm similar to the one used by {@link IntersectionChopper}: First, compute a backward slice
 * of the sink and then compute a forward slice of the source with respect to that backward slice.
 * Both slicers are context-sensitive, but this may not carry over to the chopper. To get a truly
 * context-sensitive result, you presumbly have to do something like {@link RepsRosayChopper}, but
 * on the control-flow graph.
 * @author Martin Mohr&lt;martin.mohr@kit.edu&gt;
 *
 */
public class SimpleTCFGChopper {

	private CFGForward forw;
	private CFGBackward backw;

	public SimpleTCFGChopper(CFG icfg) {
		this.forw = new CFGForward(icfg);
		this.backw = new CFGBackward(icfg);
	}

	Collection<? extends SDGNode> chop(SDGNode source, SDGNode sink) {
		return forw.subgraphSlice(Collections.singleton(source), backw.slice(sink));
	}
}
