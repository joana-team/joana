/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cfg.ExceptionPrunedCFG;
import com.ibm.wala.ipa.cfg.PrunedCFG;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;

import edu.kit.joana.deprecated.jsdg.util.GraphUtil;
import edu.kit.joana.deprecated.jsdg.util.Log;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class ExceptionFilter {

	private ExceptionFilter() {
	}

	/**
	 * Search all nodes in the given callgraph that belongs to methods that are
	 * only called through exception code. These are methods that can be ignored
	 * when we do not consider the effect of exceptions in our analysis.
	 * @param cg The callgraph.
	 * @return The set of methods that are only relevant when exceptions are not ignored.
	 */
	public static Set<CGNode> findExceptionOnlyNodes(CallGraph cg) {
		Set<CGNode> toIgnore = new HashSet<CGNode>();
		MutableIntSet notIgnored = new BitVectorIntSet();
		notIgnored.add(cg.getNumber(cg.getFakeRootNode()));

		for (CGNode cur : cg) {
			if (isExceptionInit(cg, cur)) {
				// skipping exception initializers when exceptions are ignored
				Log.info("Ignoring exception initializer " + cur.getMethod());
				toIgnore.add(cur);
			} else if (!calledOutsideOfCatchBlock(cg, cur)) {
				Log.info("Ignoring method only called from within catch block " + cur.getMethod());
				toIgnore.add(cur);
			} else {
				notIgnored.add(cg.getNumber(cur));
			}
		}


		// add methods that are only called from ignored methods
		NumberedGraph<CGNode> pruned = GraphUtil.createSubGraph(cg, notIgnored);
		NumberedGraph<CGNode> reachableFromPruned = GraphUtil.createReachableFromSubGraph(pruned, cg.getFakeRootNode());

		for (CGNode reach : cg) {
			if (!reachableFromPruned.containsNode(reach) && !toIgnore.contains(reach)) {
				Log.info("Removing " + reach + " as it is not reachable anymore.");
				toIgnore.add(reach);
			}
		}

		return toIgnore;
	}

	private static boolean isExceptionInit(CallGraph cg, CGNode node) {
        if (node.getMethod().isInit()) {
            IClassHierarchy cha = cg.getClassHierarchy();
            IClass klass = node.getMethod().getDeclaringClass();
            IClass throwable = cha.lookupClass(TypeReference.JavaLangThrowable);
            return cha.isSubclassOf(klass, throwable);
        } else {
        	return false;
        }
	}

	private static boolean calledOutsideOfCatchBlock(CallGraph cg, CGNode n) {
		for (Iterator<CGNode> pred = cg.getPredNodes(n); pred.hasNext();) {
			CGNode caller = pred.next();
			if (caller.getIR() == null) {
				continue;
			}

			for (Iterator<CallSiteReference> sites = cg.getPossibleSites(caller, n); sites.hasNext();) {
				CallSiteReference csr = sites.next();
				ISSABasicBlock[] blocks = caller.getIR().getBasicBlocksForCall(csr);
				for (ISSABasicBlock bb : blocks) {
					if (!dominatedByCatchBlock(cg, caller, bb, n)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private static boolean dominatedByCatchBlock(CallGraph cg, CGNode node, ISSABasicBlock bb, CGNode called) {
		SSACFG cfg = node.getIR().getControlFlowGraph();
		PrunedCFG<SSAInstruction, ISSABasicBlock> pCfg = ExceptionPrunedCFG.make(cfg);

		if (!pCfg.containsNode(bb)) {
			Log.info("BB not in pruned cfg of " + node + " while call to " + called);
			return true;
		} else {
			IntSet reach = GraphUtil.findNodesReachingTo(pCfg, bb);
			int entryNum = pCfg.getNumber(pCfg.entry());

			// When the basic block bb can not be reached from the entry through normal (non-exception) cfg edges,
			// then a catch block dominates bb.
			boolean dominatedByCatch = ! reach.contains(entryNum);
			if (dominatedByCatch) {
				Log.info("ENTRY not reaching to basic block calling " + called + " inside of " + node);
			}

			return dominatedByCatch;
		}
	}
}
