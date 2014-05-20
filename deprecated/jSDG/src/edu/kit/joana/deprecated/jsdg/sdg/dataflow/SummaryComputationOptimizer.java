/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.dataflow;

import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.GraphReachability;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG.Call;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamSet;
import edu.kit.joana.deprecated.jsdg.util.Log;

/**
 * Tries to speed up summary edge computation by inlining dependencies for
 * recursive methods.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
@SuppressWarnings("deprecation")
public class SummaryComputationOptimizer {

	private final SDG sdg;

	private SummaryComputationOptimizer(final SDG sdg) {
		this.sdg = sdg;
	}

	public static Set<AbstractPDGNode> run(final SDG sdg, final IProgressMonitor progress) throws CancelException {
		SummaryComputationOptimizer opt = new SummaryComputationOptimizer(sdg);
		return opt.compute(progress);
	}

	private Set<AbstractPDGNode> compute(IProgressMonitor progress) throws CancelException {
		Set<AbstractPDGNode> toInline = HashSetFactory.make();

		CallGraph cg = sdg.getCallGraph();

		GraphReachability<CGNode,CGNode> reach = new GraphReachability<CGNode,CGNode>(cg, new TrueFilter());
		progress.subTask("Searching recursive methods");
		reach.solve(progress);

		Set<CGNode> recursive = HashSetFactory.make();

		// find all methods that may recursively call themselves (direct or indirect)
		for (CGNode node : cg) {

			// detect indirect recursion
			OrdinalSet<CGNode> rset = reach.getReachableSet(node);
			for (CGNode target : rset) {
				if (target != node) {
					OrdinalSet<CGNode> tset = reach.getReachableSet(target);
					if (tset.contains(node)) {
						recursive.add(node);
						break;
					}
				}
			}

			// detect direct self recursion
			if (cg.getSuccNodeNumbers(node).contains(cg.getNumber(node))) {
				recursive.add(node);
			}

			progress.worked(1);
		}

		int num = 0;

		// search for calls to method that may end back in the calling method
		for (Call call : sdg.getAllCalls()) {
			CGNode callee = call.callee.getCallGraphNode();
			CGNode caller = call.caller.getCallGraphNode();

			if (recursive.contains(callee) && recursive.contains(caller)) {
				OrdinalSet<CGNode> reachable = reach.getReachableSet(callee);
				if (reachable.contains(caller)) {
					toInline.add(call.node);
					num++;

					IParamSet<? extends AbstractParameterNode> aOuts =
						call.caller.getParamModel().getModParams(call.node);
					IParamSet<? extends AbstractParameterNode> aIns =
						call.caller.getParamModel().getRefParams(call.node);

					for (AbstractParameterNode aOut : aOuts) {
						toInline.add(aOut);
					}

					for (AbstractParameterNode aIn : aIns) {
						toInline.add(aIn);
					}
				}
			}
		}

		Log.info("Summary opt inlined " + num + " calls.");

		progress.done();

		return toInline;
	}

	private static class TrueFilter implements Filter<CGNode> {

		public boolean accepts(CGNode o) {
			return true;
		}

	}

}
