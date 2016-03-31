package edu.kit.joana.ifc.sdg.irlsod;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.IntersectionChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGForward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.util.Pair;

/**
 * This is a simple chopper for threaded interprocedural control-flow graphs which employs an algorithm similar to the
 * one used by {@link IntersectionChopper}: First, compute a backward slice of the sink and then compute a forward slice
 * of the source with respect to that backward slice. Both slicers are context-sensitive, but this may not carry over to
 * the chopper. To get a truly context-sensitive result, you presumbly have to do something like
 * {@link RepsRosayChopper}, but on the control-flow graph.
 *
 * @author Martin Mohr&lt;martin.mohr@kit.edu&gt;
 *
 */
public class SimpleTCFGChopper {

	private final CFGForward forw;
	private final CFGBackward backw;

	private final Map<SDGNode, Collection<SDGNode>> bwCache = new HashMap<SDGNode, Collection<SDGNode>>();
	private final Map<Pair<SDGNode, SDGNode>, Collection<SDGNode>> chCache = new HashMap<Pair<SDGNode, SDGNode>, Collection<SDGNode>>();

	public SimpleTCFGChopper(final CFG icfg) {
		this.forw = new CFGForward(icfg);
		this.backw = new CFGBackward(icfg);
	}

	Collection<? extends SDGNode> chop(final SDGNode source, final SDGNode sink) {
		Collection<SDGNode> ret = chCache.get(Pair.pair(source, sink));
		if (ret == null) {
			Collection<SDGNode> bwSlice = bwCache.get(sink);
			if (bwSlice == null) {
				bwSlice = backw.slice(sink);
				bwCache.put(sink, bwSlice);
			}
			ret = forw.subgraphSlice(Collections.singleton(source), bwSlice);
			chCache.put(Pair.pair(source, sink), ret);
		}
		return ret;
	}
}
