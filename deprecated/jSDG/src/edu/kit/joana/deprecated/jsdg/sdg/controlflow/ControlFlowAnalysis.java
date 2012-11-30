/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.controlflow;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.deprecated.jsdg.Messages;
import edu.kit.joana.deprecated.jsdg.exceptions.ExceptionPrunedCFGAnalysis;
import edu.kit.joana.deprecated.jsdg.sdg.IntermediatePDG;
import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.dataflow.CFGWithParameterNodes;
import edu.kit.joana.deprecated.jsdg.sdg.dataflow.CFGWithParameterNodes.CFGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.EdgeType;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.util.Debug;
import edu.kit.joana.deprecated.jsdg.util.Util;

/**
 * Maps the control flow of the intermediate representation to the nodes of our SDG.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ControlFlowAnalysis {

	private final SDG sdg;
	private final ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock> epa;

	public static void compute(SDG sdg, ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock> epa,
			IProgressMonitor progress) throws PDGFormatException, CancelException {
		ControlFlowAnalysis cfa = new ControlFlowAnalysis(sdg, epa);
		progress.subTask(Messages.getString("SDG.SubTask_Add_Control_Flow"));
		cfa.compute(progress);
	}

	private ControlFlowAnalysis(SDG sdg, ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock> epa) {
		this.sdg = sdg;
		this.epa = epa;
	}

	private void compute(IProgressMonitor progress) throws PDGFormatException, CancelException {
		for (PDG pdg : sdg.getAllContainedPDGs()) {
	        if (progress.isCanceled()) {
	            throw CancelException.make("Operation aborted.");
	        }

			/*
			 * In order to conform to walas policy we do not add control flow
			 * edges from entry to exit in our cfgs. The wala control dependency computation
			 * is patched to create appropriate graphs without these virtual edges.
			 * So we have to add them manually here (and not automagically by
			 * traversing ExplodedCFGWithParameterNodes...)
			 */

			pdg.addControlFlowDependency(pdg.getRoot(), pdg.getExit());

			if (pdg.isStub() || (pdg.getIR() == null && pdg.getMethod().isNative())) {
				// No IR for native method -> we have to skip the heap data
				// dependencies and build control flow manually
				AbstractPDGNode last = pdg.getRoot();
				for (AbstractParameterNode param : pdg.getParamModel().getRefParams()) {
					pdg.addControlFlowDependency(last, param);
					last = param;
				}
				for (AbstractParameterNode param : pdg.getParamModel().getModParams()) {
					if (param == pdg.getExit() || param == pdg.getExceptionalExit()) {
						continue;
					}

					pdg.addControlFlowDependency(last, param);
					last = param;
				}
				if (!sdg.isIgnoreExceptions() && pdg.getExceptionalExit() != null) {
					pdg.addControlFlowDependency(last, pdg.getExceptionalExit());
					last = pdg.getExceptionalExit();
				}

				pdg.addControlFlowDependency(last, pdg.getExit());
			} else {
				try {
					computeControlFlow(pdg, progress);
				} catch (UnsoundGraphException ge) {
					throw new PDGFormatException(ge);
				}
			}

			progress.worked(1);
		}

		AbstractPDGNode last = sdg.getRoot();
		// Add control flow to SDG Entry -> Static calls
		for (PDG pdg : sdg.getStaticInitializers()) {
			CallNode call = null;
			Iterator<? extends AbstractPDGNode> it = sdg.getPredNodes(pdg.getRoot(), EdgeType.CL);
			while (it.hasNext()) {
				call = (CallNode) it.next();
				if (call.isStaticRootCall()) {
					break;
				} else {
					call = null;
				}
			}

			sdg.addControlFlowDependency(last, call);
			last = call;
		}

		Iterator<? extends AbstractPDGNode> it = sdg.getPredNodes(sdg.getMainPDG().getRoot(), EdgeType.CL);
		CallNode mainCall = null;
		while (it.hasNext()) {
			mainCall = (CallNode) it.next();
			if (mainCall.isStaticRootCall()) {
				break;
			} else {
				mainCall = null;
			}
		}

		sdg.addControlFlowDependency(last, mainCall);
		sdg.addControlFlowDependency(mainCall, sdg.getExit());

		progress.done();
	}


	private void computeControlFlow(IntermediatePDG pdg, IProgressMonitor progress) throws UnsoundGraphException, CancelException {
		ExplodedControlFlowGraph explCfg = ExplodedControlFlowGraph.make(pdg.getIR());
		IgnoreNonTerminationCFG<SSAInstruction, IExplodedBasicBlock> igNonTermCfg;

		if (!pdg.isIgnoreExceptions() && epa != null) {
			// use exception prune analysis from zea
			ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg = epa.getPruned(pdg.getCallGraphNode(), progress);
			igNonTermCfg = new IgnoreNonTerminationCFG<SSAInstruction, IExplodedBasicBlock>(cfg, pdg.isIgnoreExceptions());
		} else {
			igNonTermCfg = new IgnoreNonTerminationCFG<SSAInstruction, IExplodedBasicBlock>(explCfg, pdg.isIgnoreExceptions());
		}

		CFGWithParameterNodes ecfg = new CFGWithParameterNodes(igNonTermCfg, pdg);

		if (Debug.Var.DUMP_CFG.isSet()) {
			Util.dumpCFGorigExpl(explCfg, null);
			Util.dumpCFGorigExpl(igNonTermCfg, "nonterm-", null);
			Util.dumpCFG(ecfg, null);
		}

		addControlFlowToPDG(pdg, ecfg);
	}

	private static final void addControlFlowToPDG(IntermediatePDG pdg, CFGWithParameterNodes ecfg) {
		for (CFGNode cfgnode : ecfg) {
			AbstractPDGNode from = getNodeForCFGNode(pdg, cfgnode);
			if (from == null) {
				continue;
			}

			addControlFlowAndSkipSkipInstructions(pdg, ecfg, from, cfgnode);
		}

		CFGNode entry = ecfg.getEntry();
		AbstractPDGNode append = pdg.getExit();
		for (Iterator<? extends CFGNode> it = ecfg.getSuccNodes(entry); it.hasNext();) {
			CFGNode next = it.next();
			if (next.isParameter()) {
				append = next.getParameterNode();
				break;
			}
		}
		pdg.addControlFlowDependency(pdg.getRoot(), append);
	}

	/**
	 * The control flow graph may contain SKIP nodes that do not correspond to any
	 * real sdg nodes. So we have to follow the cfg until we reach cfg nodes that match
	 * a sdg node.
	 */
	private static final void addControlFlowAndSkipSkipInstructions(IntermediatePDG pdg, CFGWithParameterNodes ecfg,
			AbstractPDGNode from, CFGNode start) {
		Set<CFGNode> visited = new HashSet<CFGNode>();
		LinkedList<CFGNode> work = new LinkedList<CFGNode>();
		work.add(start);

		while (!work.isEmpty()) {
			CFGNode current = work.removeFirst();

			if (!visited.contains(current)) {
				visited.add(current);

				for (Iterator<? extends CFGNode> it = ecfg.getSuccNodes(current); it.hasNext();) {
					CFGNode next = it.next();
					AbstractPDGNode nextNode = getNodeForCFGNode(pdg, next);

					if (from == nextNode) {
						continue;
					}

					if (nextNode != null) {
						pdg.addControlFlowDependency(from, nextNode);
					} else if (!visited.contains(next)){
						work.add(next);
					}
				}
			}
		}
	}

	private static final AbstractPDGNode getNodeForCFGNode(IntermediatePDG pdg, CFGNode cfgnode) {
		AbstractPDGNode result = null;

		if (cfgnode.isParameter()) {
			result = cfgnode.getParameterNode();
		} else if (cfgnode.isBasicBlock()) {
			IExplodedBasicBlock bb = cfgnode.getBasicBlock();
			SSAInstruction instr = bb.getInstruction();

			if (bb.isEntryBlock()){
				result = pdg.getRoot();
			} else if (bb.isExitBlock()) {
				result = pdg.getExit();
			} else if (bb.isCatchBlock()) {
				SSAGetCaughtExceptionInstruction catchInstr = bb.getCatchInstruction();
				if (catchInstr != null) {
					result = pdg.getCatchForBB(catchInstr.getBasicBlockNumber());
				}
			} else if (instr != null) {
				List<AbstractPDGNode> nodes = pdg.getNodesForInstruction(instr);
				if (nodes != null && nodes.size() > 0) {
					assert nodes.size() == 1 || nodes.get(0).toString().contains("compound call");
					// assert first node is compound call node, rest of the nodes (>=2) are call nodes
					result = nodes.get(0);
				}
			}
		} else if (cfgnode.isCall()) {
			result = cfgnode.getCall();
		} else if (cfgnode.isArtificial()) {
			result = cfgnode.getArtificialNode();
		} else {
			throw new IllegalStateException();
		}

		return result;
	}


}
