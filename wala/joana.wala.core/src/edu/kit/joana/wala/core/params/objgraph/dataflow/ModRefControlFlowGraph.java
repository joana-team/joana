/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.dataflow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.INodeWithNumber;
import com.ibm.wala.util.graph.impl.SparseNumberedGraph;

import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGField;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.params.objgraph.ModRefCandidates;
import edu.kit.joana.wala.core.params.objgraph.ModRefFieldCandidate;
import edu.kit.joana.wala.core.params.objgraph.ModRefCandidates.InterProcCandidateModel;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class ModRefControlFlowGraph extends SparseNumberedGraph<ModRefControlFlowGraph.Node> {

	public static final class Node implements INodeWithNumber {

		private int id;
		private final ModRefFieldCandidate cand;
		private final PDGNode node;
		private final Kind kind;

		public enum Kind { NOP, READ, WRITE, FORMAL_IN, FORMAL_OUT, ACTUAL_IN, ACTUAL_OUT }

		private Node(final int id, final PDGNode node, final Kind kind) {
			this(id, null, node, kind);
		}

		private Node(final int id, final ModRefFieldCandidate cand, final PDGNode node, final Kind kind) {
			this.id = id;
			this.cand = cand;
			this.node = node;
			this.kind = kind;
		}

		public boolean isMod() {
			if (kind == Kind.FORMAL_IN || kind == Kind.ACTUAL_OUT) {
				return true;
			} else if (kind == Kind.FORMAL_OUT || kind == Kind.ACTUAL_IN) {
				return false;
			} else {
				return (cand != null ? cand.isMod() : false);
			}
		}

		public boolean isRef() {
			if (kind == Kind.FORMAL_IN || kind == Kind.ACTUAL_OUT) {
				return false;
			} else if (kind == Kind.FORMAL_OUT || kind == Kind.ACTUAL_IN) {
				return true;
			} else {
				return (cand != null ? cand.isRef() : false);
			}
		}

		public Kind getKind() {
			return kind;
		}

		@Override
		public int getGraphNodeId() {
			return id;
		}

		@Override
		public void setGraphNodeId(final int number) {
			this.id = number;
		}

		public boolean isNOP() {
			assert cand == null || kind != Kind.NOP;
			return kind == Kind.NOP;
		}

		public ModRefFieldCandidate getCandidate() {
			return cand;
		}

		public PDGNode getNode() {
			return node;
		}

		public int hashCode() {
			return id;
		}

		public boolean equals(final Object obj) {
			if (obj instanceof Node) {
				return id == ((Node) obj).id;
			}

			return false;
		}

		public String toString() {
			return (cand == null ? node.toString() : cand.toString());
		}

	}

	private int currentId = 0;

	private Node entry;
	private Node exit;

	private ModRefControlFlowGraph() {}

	public static ModRefControlFlowGraph compute(final ModRefCandidates modref, final PDG pdg, final CallGraph cg,
			final IProgressMonitor progress) {
		final ModRefControlFlowGraph cfg = new ModRefControlFlowGraph();

		cfg.run(modref, pdg, cg, progress);

		return cfg;
	}

	public Node getEntry() {
		return entry;
	}

	public Node getExit() {
		return exit;
	}

	private Node createNode(final ModRefFieldCandidate cand, final PDGNode node, final Node.Kind kind) {
		final Node n = new Node(currentId++, cand, node, kind);
		addNode(n);

		return n;
	}

	private Node createNOPNode(final PDGNode node) {
		final Node n = new Node(currentId++, null, node, Node.Kind.NOP);
		addNode(n);

		return n;
	}

	private void run(final ModRefCandidates modref, final PDG pdg, final CallGraph cg,
			final IProgressMonitor progress) {
		final Map<PDGNode, Node> pdg2cfg = new HashMap<PDGNode, Node>();
		createInitalNodesAndFlow(pdg2cfg, modref, pdg);
		createFormalInAndOut(pdg2cfg, modref, pdg);
		createActualInAndOut(pdg2cfg, modref, pdg, cg);
	}

	private void createInitalNodesAndFlow(final Map<PDGNode, Node> pdg2cfg, final ModRefCandidates modref,
			final PDG pdg) {
		final Node nEntry = createNOPNode(pdg.entry);
		this.entry = nEntry;
		pdg2cfg.put(pdg.entry, nEntry);
		final Node nExit = createNOPNode(pdg.exit);
		this.exit = nExit;
		pdg2cfg.put(pdg.exit, nExit);

		for (final PDGField fref : pdg.getFieldReads()) {
			if (!fref.field.isStatic()) {
				final SSAInstruction instr = pdg.getInstruction(fref.node);
				final ModRefFieldCandidate refCand = modref.createRefCandidate(pdg.cgNode, instr);
				final Node n = createNode(refCand, fref.accfield, Node.Kind.READ);
				pdg2cfg.put(fref.accfield, n);
			}
		}

		for (final PDGField fmod : pdg.getFieldWrites()) {
			if (!fmod.field.isStatic()) {
				final SSAInstruction instr = pdg.getInstruction(fmod.node);
				final ModRefFieldCandidate refCand = modref.createModCandidate(pdg.cgNode, instr);
				final Node n = createNode(refCand, fmod.accfield, Node.Kind.WRITE);
				pdg2cfg.put(fmod.accfield, n);
			}
		}

		for (final PDGNode pn : pdg.vertexSet()) {
			if (pn.getPdgId() == pdg.getId() && !pdg2cfg.containsKey(pn)) {
				final Node n = createNOPNode(pn);
				pdg2cfg.put(pn, n);
			}
		}

		for (final PDGEdge e : pdg.edgeSet()) {
			if (e.kind == PDGEdge.Kind.CONTROL_FLOW || e.kind == PDGEdge.Kind.CONTROL_FLOW_EXC) {
				final Node from = pdg2cfg.get(e.from);
				final Node to = pdg2cfg.get(e.to);
				addEdge(from, to);
			}
		}
	}

	private void createFormalInAndOut(final Map<PDGNode, Node> pdg2cfg, final ModRefCandidates modref, final PDG pdg) {
		final InterProcCandidateModel pdgModRef = modref.getCandidates(pdg.cgNode);

		if (pdgModRef == null) { return; }

		final List<ModRefFieldCandidate> ref = new LinkedList<ModRefFieldCandidate>();
		final List<ModRefFieldCandidate> mod = new LinkedList<ModRefFieldCandidate>();
		for (final ModRefFieldCandidate c : pdgModRef) {
			if (c.isMod()) {
				mod.add(c);
			}

			if (c.isRef()) {
				ref.add(c);
			}
		}

		addNodesAfter(entry, ref, Node.Kind.FORMAL_IN);
		addNodesBefore(exit, mod, Node.Kind.FORMAL_OUT);
	}

	private void createActualInAndOut(final Map<PDGNode, Node> pdg2cfg, final ModRefCandidates modref, final PDG pdg,
			final CallGraph cg) {
		for (final PDGNode call : pdg.getCalls()) {
			final Node cn = pdg2cfg.get(call);
			final SSAInvokeInstruction invk = (SSAInvokeInstruction) pdg.getInstruction(call);
			final CallSiteReference site = invk.getCallSite();

			final Set<ModRefFieldCandidate> modRefCands = new HashSet<ModRefFieldCandidate>();

			for (final CGNode tgt : cg.getPossibleTargets(pdg.cgNode, site)) {
				final InterProcCandidateModel tgtModRef = modref.getCandidates(tgt);

				if (tgtModRef == null) { continue; }

				for (final ModRefFieldCandidate c : tgtModRef) {
					modRefCands.add(c);
				}
			}

			final List<ModRefFieldCandidate> refs = new LinkedList<ModRefFieldCandidate>();
			final List<ModRefFieldCandidate> mods = new LinkedList<ModRefFieldCandidate>();

			for (final ModRefFieldCandidate c : modRefCands) {
				if (c.isMod()) {
					mods.add(c);
				}

				if (c.isRef()) {
					refs.add(c);
				}
			}

			addNodesBefore(cn, refs, Node.Kind.ACTUAL_IN);
			addNodesAfter(cn, mods, Node.Kind.ACTUAL_OUT);
		}
	}

	private void addNodesBefore(final Node n, final List<ModRefFieldCandidate> cands, final Node.Kind kind) {
		if (cands == null || cands.isEmpty()) { return; }

		final LinkedList<Node> preds = new LinkedList<Node>();
		final Iterator<Node> itPred = getPredNodes(n);
		while (itPred.hasNext()) {
			final Node pred = itPred.next();
			preds.addLast(pred);
		}

		for (final Node pred : preds) {
			removeEdge(pred, n);
		}

		Node last = n;
		for (final ModRefFieldCandidate c : cands) {
			final Node next = createNode(c, n.node, kind);
			addEdge(next, last);
			last = next;
		}

		for (final Node pred : preds) {
			addEdge(pred, last);
		}
	}

	private void addNodesAfter(final Node n, final List<ModRefFieldCandidate> cands, final Node.Kind kind) {
		if (cands == null || cands.isEmpty()) { return; }

		final List<Node> succs = new LinkedList<Node>();
		final Iterator<Node> itSucc = getSuccNodes(n);
		while (itSucc.hasNext()) {
			final Node succ = itSucc.next();
			succs.add(succ);
		}

		for (final Node succ : succs) {
			removeEdge(n, succ);
		}

		Node last = n;
		for (final ModRefFieldCandidate c : cands) {
			final Node next = createNode(c, n.node, kind);
			addEdge(last, next);
			last = next;
		}

		for (final Node succ : succs) {
			addEdge(last, succ);
		}
	}

}
