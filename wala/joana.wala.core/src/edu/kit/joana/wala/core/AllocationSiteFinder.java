/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.

 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * Finds the possible allocation sites for interesting calls.
 * @author Martin Mohr
 */
public abstract class AllocationSiteFinder {

	protected final SDGBuilder builder;
	/** maps calls to the possible allocation sites of the object on which the respective method is called */
	private final Map<PDGNode, TIntSet> node2alloc = new HashMap<PDGNode, TIntSet>();
	private boolean computationFinished = false;
	protected static final Logger debug = Log.getLogger(Log.L_WALA_CORE_DEBUG);

	public AllocationSiteFinder(SDGBuilder builder) {
		this.builder = builder;
	}

	public Map<PDGNode, TIntSet> getAllocationSites() {
		if (!computationFinished) {
			run();
			computationFinished = true;
		}
		return node2alloc;
	}

	private void run() {
		for (PDG pdg : builder.getAllPDGs()) {
			for (PDGNode call : pdg.getCalls()) {
				assert pdg.getInstruction(call) instanceof SSAAbstractInvokeInstruction: "call node without call instruction?!";
				SSAAbstractInvokeInstruction invk = (SSAAbstractInvokeInstruction) pdg.getInstruction(call);
				if (invk.isDispatch() && isInterestingCall(pdg, call)) {
					final Set<PDGNode> allocNodes = findAllocNodes(builder, pdg.cgNode, pdg.getInstruction(call));
					TIntSet allocIds = new TIntHashSet();
					for (PDGNode n : allocNodes) {
						allocIds.add(n.getId());
					}

					node2alloc.put(call, allocIds);
				}
			}
		}
	}

	protected abstract boolean isInterestingCall(PDG callingCtx, PDGNode call);

	private static final Set<PDGNode> findAllocNodes(SDGBuilder builder, CGNode method, SSAInstruction callInstruction) {
		final SSAInvokeInstruction invk = (SSAInvokeInstruction) callInstruction;
		final Set<PDGNode> alloc = new HashSet<PDGNode>();
		final int ssaVar = invk.getReceiver();
		PointerAnalysis pts = builder.getPointerAnalysis();
		final PointerKey pk = pts.getHeapModel().getPointerKeyForLocal(method, ssaVar);
		final OrdinalSet<InstanceKey> ptsSet = pts.getPointsToSet(pk);
		for (final InstanceKey ik : ptsSet) {
			if (ik instanceof AllocationSiteInNode) {
				final AllocationSiteInNode asin = (AllocationSiteInNode) ik;
				final CGNode node = asin.getNode();
				final PDG allocPdg = builder.getPDGforMethod(node);
				if (allocPdg == null) {
					continue;
				}
				final NewSiteReference nsr = asin.getSite();
				final PDGNode allocNode = findDeclarationForBytecodeIndex(allocPdg, nsr.getProgramCounter());

				if (allocNode != null) {
					//System.err.println("Found alloc node: " + allocNode);
					alloc.add(allocNode);
					//				} else {
					//					System.err.println("No node found for: " + asin);
				}
			}
		}

		return alloc;
	}

	private static final PDGNode findDeclarationForBytecodeIndex(final PDG pdg, final int bcIndex) {
		for (final PDGNode n : pdg.vertexSet()) {
			if (n.getKind() == PDGNode.Kind.NEW && n.getBytecodeIndex() == bcIndex) {
				return n;
			}
		}
		return null;
	}
}