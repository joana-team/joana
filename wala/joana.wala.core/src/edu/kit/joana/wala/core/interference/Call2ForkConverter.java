/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.interference;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.collections.Pair;

import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGEdge.Kind;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.SDGBuilder;

/**
 * Converts all calls from Thread.start() to a thread entry into forks.
 * Also removes the CALL_RET node at each such call sites. This reflects the property that
 * a spawn does not return.
 * @author Martin Mohr
 *
 */
public class Call2ForkConverter {

	private static final Logger debug = Log.getLogger(Log.L_WALA_INTERFERENCE_DEBUG);
	private static final boolean IS_DEBUG = debug.isEnabled();
	
	private static class RemovalTask {
		private final PDG pdgToModify;
		private final PDGNode nodeToRemove;
		private final Set<PDG> possibleTargets;

		RemovalTask(PDG pdgToModify, PDGNode nodeToRemove, Set<PDG> possibleTargets) {
			assert pdgToModify != null;
			assert nodeToRemove != null;
			assert nodeToRemove.getBytecodeIndex() == BytecodeLocation.CALL_RET;
			this.pdgToModify = pdgToModify;
			this.nodeToRemove = nodeToRemove;
			this.possibleTargets = possibleTargets;
		}

		void execute() {
			// connect every predecessor with every successor
			List<PDGEdge> edgesToAdd = new LinkedList<PDGEdge>();
			for (PDGEdge incEdge : pdgToModify.incomingEdgesOf(nodeToRemove)) {
				for (PDGEdge outgEdge : pdgToModify.outgoingEdgesOf(nodeToRemove)) {
					if (incEdge.kind == outgEdge.kind) {
						assert outgEdge.to != null;
						edgesToAdd.add(new PDGEdge(incEdge.from, outgEdge.to, incEdge.kind));
					}
				}
			}

			for (PDGEdge e : edgesToAdd) {
				if (IS_DEBUG) debug.outln("Adding edge " + e.from + " -" + e.kind + "-> " + e.to);
				pdgToModify.addEdge(e.from, e.to, e.kind);
			}

			// remove all outgoing edges
			List<PDGEdge> edgesToRemove = new LinkedList<PDGEdge>();
			edgesToRemove.addAll(pdgToModify.outgoingEdgesOf(nodeToRemove));

			for (PDGEdge e : edgesToRemove) {
				if (IS_DEBUG) debug.outln("Removing edge " + e.from + " -" + e.kind + "-> " + e.to);
				pdgToModify.removeEdge(e);
			}

			List<Pair<PDG, PDGEdge>> removeLater = new LinkedList<Pair<PDG, PDGEdge>>();

			// remove all incoming edges
			for (PDGEdge e : pdgToModify.incomingEdgesOf(nodeToRemove)) {
				removeLater.add(Pair.make(pdgToModify, e));
			}

			for (PDG possTgt : possibleTargets) {
				for (PDGEdge e : possTgt.edgeSet()) {
					if (e.to.equals(nodeToRemove)) {
						removeLater.add(Pair.make(possTgt, e));
					}
				}
			}

			for (Pair<PDG, PDGEdge> p : removeLater) {
				PDG pdg = p.fst;
				PDGEdge e = p.snd;
				String interProc = "";
				if (e.from.getPdgId() != e.to.getPdgId()) {
					interProc = "interprocedural";
				}
				if (IS_DEBUG) debug.outln("Removing " + interProc + " edge " + e.from + " -" + e.kind + "-> " + e.to);
				pdg.removeEdge(e);
			}

			assert pdgToModify.outgoingEdgesOf(nodeToRemove).isEmpty();
			assert pdgToModify.incomingEdgesOf(nodeToRemove).isEmpty();

			for (PDG possTgt : possibleTargets) {
				for (PDGEdge e : possTgt.edgeSet()) {
					assert !e.to.equals(nodeToRemove);
				}
			}

			// remove node
			if (IS_DEBUG) debug.outln("Removing node " + nodeToRemove);
			pdgToModify.removeNode(nodeToRemove);

		}
	}

	private static class ConversionTask {

		private final PDG pdgToModify;
		private final PDGEdge edgeToModify;

		ConversionTask(PDG pdgToModify, PDGEdge edgeToModify) {
			assert pdgToModify != null;
			assert edgeToModify != null;
			assert edgeToModify.kind == PDGEdge.Kind.CALL_STATIC || edgeToModify.kind == PDGEdge.Kind.CALL_VIRTUAL
					|| edgeToModify.kind == PDGEdge.Kind.PARAMETER_IN
					|| edgeToModify.kind == PDGEdge.Kind.PARAMETER_OUT : edgeToModify.kind;
			this.pdgToModify = pdgToModify;
			this.edgeToModify = edgeToModify;
		}

		void execute() {

			pdgToModify.removeEdge(edgeToModify);
			if (IS_DEBUG) debug.outln("Removing edge " + edgeToModify);

			if (edgeToModify.kind != PDGEdge.Kind.PARAMETER_OUT) {
				PDGEdge newEdge;
				if (edgeToModify.kind == PDGEdge.Kind.CALL_STATIC || edgeToModify.kind == PDGEdge.Kind.CALL_VIRTUAL) {
					newEdge = new PDGEdge(edgeToModify.from, edgeToModify.to, PDGEdge.Kind.FORK);
				} else {
					assert edgeToModify.kind == PDGEdge.Kind.PARAMETER_IN;
					newEdge = new PDGEdge(edgeToModify.from, edgeToModify.to, PDGEdge.Kind.FORK_IN);
				}
				if (IS_DEBUG) debug.outln("Adding edge " + newEdge);
				pdgToModify.addEdge(newEdge.from, newEdge.to, newEdge.kind);
			}

			// check that no pdg edge is malicious
			for (PDGEdge e : pdgToModify.edgeSet()) {
				assert e.from != null;
				assert e.to != null;
				assert e.kind != null;
			}

		}
	}

	private final SDGBuilder builder;
	private final ThreadInformationProvider tiProvider;

	public Call2ForkConverter(SDGBuilder builder, ThreadInformationProvider tiProvider) {
		super();
		this.builder = builder;
		this.tiProvider = tiProvider;
	}

	public void run() {
		if (IS_DEBUG) debug.outln("Converting calls of Thread.run() from Thread.start() to forks...");
		List<ConversionTask> conversionTasks = new LinkedList<ConversionTask>();
		List<RemovalTask> removalTasks = new LinkedList<RemovalTask>();

		Set<CGNode> threadStarts = tiProvider.getAllThreadStartNodesInCallGraph();
		Set<CGNode> threadEntries = tiProvider.getAllThreadEntryNodesInCallGraph();

		for (CGNode cgThreadStart : threadStarts) {
			PDG pdgOfThreadStart = builder.getPDGforMethod(cgThreadStart);
			for (PDGNode call : pdgOfThreadStart.getCalls()) {
				// TODO: only consider calls of Thread.run() (--> modify ThreadInformationProvider such that it provides that information)
				if (!tiProvider.isCallOfThreadRunOverriding(pdgOfThreadStart, call)) {
					continue;
				}

				PDGNode callRet = null;
				for (PDGEdge outgEdge : pdgOfThreadStart.outgoingEdgesOf(call)) {
					if (outgEdge.to.getBytecodeIndex() == BytecodeLocation.CALL_RET) {
						callRet = outgEdge.to;
						break;
					}
				}

				if (callRet != null) {
					removalTasks.add(new RemovalTask(pdgOfThreadStart, callRet, builder.getPossibleTargets(call)));
				}

				for (PDG calledPDG : builder.getPossibleTargets(call)) {
					if (threadEntries.contains(calledPDG.cgNode)) {
						conversionTasks.addAll(collectParamOutsFromEntry(calledPDG.entry, pdgOfThreadStart, calledPDG));
						for (PDGEdge edge : pdgOfThreadStart.outgoingEdgesOf(call)) {
							if (edge.kind == Kind.CALL_STATIC || edge.kind == Kind.CALL_VIRTUAL) {
								conversionTasks.add(new ConversionTask(pdgOfThreadStart, edge));
							} else if (edge.kind == Kind.CONTROL_DEP_EXPR
									&& edge.to.getKind() == PDGNode.Kind.ACTUAL_IN) {
								conversionTasks.addAll(collectParamInsFromActIn(edge.to, pdgOfThreadStart, calledPDG));
							}
						}
					}
				}
			}
		}

		for (ConversionTask task : conversionTasks) {
			task.execute();
		}

		for (RemovalTask remTask : removalTasks) {
			remTask.execute();
		}

		if (IS_DEBUG) debug.outln("done.");
	}

	private Collection<? extends ConversionTask> collectParamInsFromActIn(PDGNode actIn, PDG pdgOfThreadStart,
			PDG threadEntryPDG) {
		List<ConversionTask> tasks = new LinkedList<ConversionTask>();
		for (PDGEdge edge : pdgOfThreadStart.outgoingEdgesOf(actIn)) {
			if (edge.kind == PDGEdge.Kind.PARAMETER_IN && threadEntryPDG.vertexSet().contains(edge.to)) {
				tasks.add(new ConversionTask(pdgOfThreadStart, edge));
			}
		}

		return tasks;
	}

	private Collection<? extends ConversionTask> collectParamOutsFromEntry(PDGNode entry, PDG pdgOfThreadStart,
			PDG threadEntryPDG) {
		List<ConversionTask> tasks = new LinkedList<ConversionTask>();
		for (PDGEdge edge : threadEntryPDG.outgoingEdgesOf(entry)) {
			if (edge.kind == PDGEdge.Kind.CONTROL_DEP_EXPR && edge.to.getKind() == PDGNode.Kind.FORMAL_OUT) {
				PDGNode formalOut = edge.to;
				for (PDGEdge possibleParamOutEdge : threadEntryPDG.outgoingEdgesOf(formalOut)) {
					if (possibleParamOutEdge.kind == PDGEdge.Kind.PARAMETER_OUT) {
						PDGNode target = possibleParamOutEdge.to;
						if (pdgOfThreadStart.vertexSet().contains(target)) {
							tasks.add(new ConversionTask(threadEntryPDG, possibleParamOutEdge));
						}
					}
				}
			}
		}

		return tasks;
	}

}
