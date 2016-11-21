/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;

import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGEdge.Kind;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.accesspath.APUtil;

/**
 * Converts an object graph sdg into an object tree sdg, by copying shared parameter fields to separate nodes.
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public final class ObjTreeConverter {

	private final SDGBuilder sdg;

	private ObjTreeConverter(final SDGBuilder sdg) {
		this.sdg = sdg;
	}

	/**
	 * Converts an object graph sdg to an object tree sdg. This is expected to be run after object graph
	 * computation and before summary edge computation. However it should also work after summary edge computation.
	 */
	public static void convert(final SDGBuilder sdg, final IProgressMonitor progress) throws CancelException {
		final ObjTreeConverter objtree = new ObjTreeConverter(sdg);
		objtree.run(progress);
	}
	
	private void run(final IProgressMonitor progress) throws CancelException {
		if (sdg.cfg.debugAccessPath) {
			for (final PDG pdg : sdg.getAllPDGs()) {
				try {
					APUtil.writeCFGtoFile(sdg.cfg.debugAccessPathOutputDir, pdg, "-pretree-cfg.dot");
				} catch (WalaException e) {
					e.printStackTrace();
				}
			}
		}
		
		final Map<PDG, Map<PDGNode, Set<PDGNode>>> pdg2succEntry = extractEntrySuccs();
		final Map<PDG, Map<PDGNode, Set<PDGNode>>> pdg2predExit = extractExitPreds();
		final Map<PDG, TreeRoots> pdgRoots = computeFormalTrees(progress);

		for (final PDG caller : sdg.getAllPDGs()) {
			deleteOrigNodes(caller, pdgRoots.get(caller));

			for (final PDGNode call : caller.getCalls()) {
				final TreeRoots callRoots = computeActualTrees(caller, call);
				deleteOrigNodes(caller, callRoots);

				for (final PDG callee : sdg.getPossibleTargets(call)) {
					final TreeRoots calleeRoots = pdgRoots.get(callee);
					final Map<PDGNode, PDGNode> formToAct = findRootFormToAct(caller, call, callee);

					for (final PDGNode formRoot : calleeRoots.getRoots()) {
						final PDGNode actRoot = formToAct.get(formRoot);
						final TreeElem actTreeRoot = callRoots.getRootTree(actRoot);
						final TreeElem formTreeRoot = calleeRoots.getRootTree(formRoot);

						// connect nodes by matching structure
						connectChildren(caller, actTreeRoot, callee, formTreeRoot);
					}
				}
			}
		}
		
		// remove spurious nodes that are no longer part of the 
		trimCallsiteTrees();
		fixupControlFlow(pdg2succEntry, pdg2predExit);
	}
	
	private Map<PDG, Map<PDGNode, Set<PDGNode>>> extractEntrySuccs() {
		final Map<PDG, Map<PDGNode, Set<PDGNode>>> pdg2succ = new HashMap<PDG, Map<PDGNode, Set<PDGNode>>>();

		for (final PDG pdg : sdg.getAllPDGs()) {
			final Map<PDGNode, Set<PDGNode>> pdgMap = new HashMap<>();
			pdg2succ.put(pdg, pdgMap);
			{
				final List<PDGNode> formIn = extractFormalIns(pdg);
				final Set<PDGNode> inSuccs = extractNonParameterSucc(pdg, formIn, PDGNode.Kind.FORMAL_IN);
				inSuccs.add(pdg.exception);
				pdgMap.put(pdg.entry, inSuccs);
			}
			
			for (final PDGNode call : pdg.getCalls()) {
				final List<PDGNode> actOuts = extractActualOuts(pdg, call);
				final Set<PDGNode> outSuccs = extractNonParameterSucc(pdg, actOuts, PDGNode.Kind.ACTUAL_OUT);
				pdgMap.put(call, outSuccs);
			}
		}
		
		return pdg2succ; 
	}
	
	private Map<PDG, Map<PDGNode, Set<PDGNode>>> extractExitPreds() {
		final Map<PDG, Map<PDGNode, Set<PDGNode>>> pdg2preds = new HashMap<PDG, Map<PDGNode, Set<PDGNode>>>();
		
		for (final PDG pdg : sdg.getAllPDGs()) {
			final Map<PDGNode, Set<PDGNode>> pdgMap = new HashMap<>();
			pdg2preds.put(pdg, pdgMap);
			{
				final List<PDGNode> formOut = extractFormalOuts(pdg);
				final Set<PDGNode> outPreds = extractNonParameterPreds(pdg, formOut, PDGNode.Kind.FORMAL_OUT);
				pdgMap.put(pdg.entry, outPreds);
			}
			
			for (final PDGNode call : pdg.getCalls()) {
				final List<PDGNode> actIns = extractActualIns(pdg, call);
				final Set<PDGNode> inPreds = extractNonParameterPreds(pdg, actIns, PDGNode.Kind.ACTUAL_IN);
				pdgMap.put(call, inPreds);
			}
		}
		
		return pdg2preds; 
	}
	
	private PDGNode firstNode(final PDGNode n, final PDG pdg) {
		if (n.getKind() == PDGNode.Kind.CALL) {
			final List<PDGNode> ains = extractActualIns(pdg, n);
			if (!ains.isEmpty()) {
				return ains.get(0);
			}
		}
		
		return n;
	}
	
	private PDGNode lastNode(final PDGNode n, final PDG pdg) {
		if (n.getKind() == PDGNode.Kind.CALL) {
			final List<PDGNode> aouts = extractActualOuts(pdg, n);
			if (!aouts.isEmpty()) {
				return aouts.get(aouts.size() - 1);
			}
		}
		
		return n;
	}
	
	private void fixupControlFlow(final Map<PDG, Map<PDGNode, Set<PDGNode>>> pdg2entrySuccs,
			final Map<PDG, Map<PDGNode, Set<PDGNode>>> pdg2exitPreds) {
		for (final PDG caller : sdg.getAllPDGs()) {
			final Map<PDGNode, Set<PDGNode>> pdgSuccs = pdg2entrySuccs.get(caller);
			final List<PDGNode> formIn = extractFormalIns(caller);
			final Set<PDGNode> inSuccs = pdgSuccs.get(caller.entry);
			final Map<PDGNode, Set<PDGNode>> pdgPreds = pdg2exitPreds.get(caller);
			final List<PDGNode> formOut = extractFormalOuts(caller);
			final Set<PDGNode> outPreds = pdgPreds.get(caller.entry);
			removeAnythingCF(formIn, caller);
			removeAnythingCF(formOut, caller);
			connectCFchain(formIn, caller);
			if (formIn.size() > 0) {
				final PDGNode first = formIn.get(0);
				caller.addEdge(caller.entry, first, PDGEdge.Kind.CONTROL_FLOW);
				final PDGNode last = formIn.get(formIn.size() - 1);
				for (final PDGNode is : inSuccs) {
					final PDGNode fn = firstNode(is, caller);
					caller.addEdge(last, fn, PDGEdge.Kind.CONTROL_FLOW);
				}
			}
			connectCFchain(formOut, caller);
			if (formIn.size() > 0) {
				final PDGNode first = formOut.get(0);
				for (final PDGNode op : outPreds) {
					final PDGNode ln = lastNode(op, caller);
					caller.addEdge(ln, first, PDGEdge.Kind.CONTROL_FLOW);
				}
				final PDGNode last = formOut.get(formOut.size() - 1);
				caller.addEdge(last, caller.exit, PDGEdge.Kind.CONTROL_FLOW);
			}
			
			for (final PDGNode call : caller.getCalls()) {
				fixupCallControlFlow(caller, call, pdgSuccs, pdgPreds);
			}
		}
	}
	
	private void fixupCallControlFlow(final PDG pdg, final PDGNode call, final Map<PDGNode, Set<PDGNode>> pdgSuccs,
			final Map<PDGNode, Set<PDGNode>> pdgPreds) {
		final List<PDGNode> actIn = extractActualIns(pdg, call);
		final Set<PDGNode> inPreds = pdgPreds.get(call);
		final List<PDGNode> actOut = extractActualOuts(pdg, call);
		final Set<PDGNode> outSuccs = pdgSuccs.get(call);
		removeAnythingCF(actIn, pdg);
		removeAnythingCF(actOut, pdg);
		connectCFchain(actIn, pdg);
		if (actIn.size() > 0) {
			final PDGNode first = actIn.get(0);
			for (final PDGNode pred : inPreds) {
				final PDGNode lp = lastNode(pred, pdg);
				pdg.addEdge(lp, first, PDGEdge.Kind.CONTROL_FLOW);
			}
			final PDGNode last = actIn.get(actIn.size() - 1);
			pdg.addEdge(last, call, PDGEdge.Kind.CONTROL_FLOW);
		}
		connectCFchain(actOut, pdg);
		if (actOut.size() > 0) {
			final PDGNode first = actOut.get(0);
			pdg.addEdge(call, first, PDGEdge.Kind.CONTROL_FLOW);
			final PDGNode last = actOut.get(actOut.size() - 1);
			for (final PDGNode succ : outSuccs) {
				final PDGNode fs = firstNode(succ, pdg);
				pdg.addEdge(last, fs, PDGEdge.Kind.CONTROL_FLOW);
			}
		}
	}
	
	private static void connectCFchain(final List<PDGNode> nodes, final PDG pdg) {
		PDGNode previous = null;
		for (final PDGNode n : nodes) {
			if (previous != null) {
				pdg.addEdge(previous, n, PDGEdge.Kind.CONTROL_FLOW);
			}
			previous = n;
		}
	}
	
	private static void removeAnythingCF(final List<PDGNode> nodes, final PDG pdg) {
		final List<PDGEdge> toRemove = new LinkedList<>();
		for (final PDGNode n : nodes) {
			for (final PDGEdge e : pdg.incomingEdgesOf(n)) {
				if (e.kind == PDGEdge.Kind.CONTROL_FLOW) {
					toRemove.add(e);
				}
			}
		}
		pdg.removeAllEdges(toRemove);
		toRemove.clear();
		for (final PDGNode n : nodes) {
			for (final PDGEdge e : pdg.outgoingEdgesOf(n)) {
				if (e.kind == PDGEdge.Kind.CONTROL_FLOW) {
					toRemove.add(e);
				}
			}
		}
		pdg.removeAllEdges(toRemove);
	}
	
	private Set<PDGNode> extractNonParameterSucc(final PDG pdg, final List<PDGNode> params, final PDGNode.Kind ignore) {
		final Set<PDGNode> succs = new HashSet<>();
		
		for (final PDGNode p : params) {
			for (final PDGEdge e : pdg.outgoingEdgesOf(p)) {
				if (e.kind == PDGEdge.Kind.CONTROL_FLOW && e.to.getKind() != ignore) {
					if (isNoParameter(e.to)) {
						succs.add(e.to);
					} else {
						final PDGNode par = findCEparent(pdg, e.to);
						succs.add(par);
					}
				}
			}
		}
		
		return succs;
	}
	
	private static PDGNode findCEparent(final PDG pdg, final PDGNode n) {
		switch (n.getKind()) {
		case ACTUAL_IN:
		case ACTUAL_OUT:
			// search call
			for (final PDGEdge e : pdg.incomingEdgesOf(n)) {
				if (e.kind == PDGEdge.Kind.CONTROL_DEP_EXPR) {
					return e.from;
				}
			}
			break;
		case FORMAL_IN:
			// search entry
			return pdg.entry;
		case FORMAL_OUT:
			// search exit
			return pdg.exit;
		default:
			return n;
		}
		
		return n;
	}

	private Set<PDGNode> extractNonParameterPreds(final PDG pdg, final List<PDGNode> params, final PDGNode.Kind ignore) {
		final Set<PDGNode> preds = new HashSet<>();
		
		for (final PDGNode p : params) {
			for (final PDGEdge e : pdg.incomingEdgesOf(p)) {
				if (e.kind == PDGEdge.Kind.CONTROL_FLOW && e.from.getKind() != ignore) {
					if (isNoParameter(e.from)) {
						preds.add(e.from);
					} else {
						final PDGNode par = findCEparent(pdg, e.from);
						preds.add(par);
					}
				}
			}
		}
		
		return preds;
	}

	private static boolean isNoParameter(final PDGNode n) {
		switch (n.getKind()) {
		case ACTUAL_IN:
		case ACTUAL_OUT:
		case FORMAL_IN:
		case FORMAL_OUT:
			return false;
		default:
			return true;
		}
	}
	
	private List<PDGNode> extractActualIns(final PDG pdg, final PDGNode call) {
		final List<PDGNode> actualIns = new LinkedList<>();

		for (final PDGEdge e : pdg.outgoingEdgesOf(call)) {
			if (e.kind == PDGEdge.Kind.CONTROL_DEP_EXPR && e.to.getKind() == PDGNode.Kind.ACTUAL_IN) {
				actualIns.add(e.to);
			}
		}
		
		return actualIns;
	}
	
	private List<PDGNode> extractActualOuts(final PDG pdg, final PDGNode call) {
		final List<PDGNode> actualOuts = new LinkedList<>();

		for (final PDGEdge e : pdg.outgoingEdgesOf(call)) {
			if (e.kind == PDGEdge.Kind.CONTROL_DEP_EXPR && e.to.getKind() == PDGNode.Kind.ACTUAL_OUT) {
				actualOuts.add(e.to);
			}
		}
		
		return actualOuts;
	}
	
	private List<PDGNode> extractFormalIns(final PDG pdg) {
		final List<PDGNode> formalIns = new LinkedList<>();

		for (final PDGEdge e : pdg.outgoingEdgesOf(pdg.entry)) {
			if (e.kind == PDGEdge.Kind.CONTROL_DEP_EXPR && e.to.getKind() == PDGNode.Kind.FORMAL_IN) {
				formalIns.add(e.to);
			}
		}
		
		return formalIns;
	}
	
	private List<PDGNode> extractFormalOuts(final PDG pdg) {
		final List<PDGNode> formalOuts = new LinkedList<>();

		for (final PDGEdge e : pdg.outgoingEdgesOf(pdg.entry)) {
			if (e.kind == PDGEdge.Kind.CONTROL_DEP_EXPR && e.to.getKind() == PDGNode.Kind.FORMAL_OUT) {
				formalOuts.add(e.to);
			}
		}
		
		return formalOuts;
	}
	
	private void trimCallsiteTrees() {
		for (final PDG caller : sdg.getAllPDGs()) {
			final List<PDGNode> toRemove = new LinkedList<PDGNode>();
			
			for (final PDGNode call : caller.getCalls()) {
				for (final PDGEdge e : caller.outgoingEdgesOf(call)) {
					if (e.kind == Kind.CONTROL_DEP_EXPR && e.to.getKind() == PDGNode.Kind.ACTUAL_IN) {
						// check if actual-in connects to formal-ins no longer present in their respective PDG
						for (final PDGEdge e2 : caller.outgoingEdgesOf(e.to)) {
							if (e2.kind == Kind.PARAMETER_IN) {
								final PDGNode toCheck = e2.to;
								final PDG pdg = sdg.getPDGforId(toCheck.getPdgId());
								if (!pdg.containsVertex(toCheck)) {
									toRemove.add(toCheck);
								}
							}
						}
					}
				}
			}
			
			caller.removeAllVertices(toRemove);
		}
	}
	
	private void deleteOrigNodes(final PDG pdg, final TreeRoots treeRoots) {
		for (final PDGNode root : treeRoots.getRoots()) {
			final TreeElem rootElem = treeRoots.getRootTree(root);
			deleteChildOrigNodes(pdg, rootElem);
		}
	}

	private static void deleteChildOrigNodes(final PDG pdg, final TreeElem elem) {
		if (elem.hasChildren()) {
			for (final TreeElem child : elem.getChildren()) {
				final PDGNode toDelete = child.orig;
				if (pdg.containsVertex(toDelete)) {
					if (isFormalParam(toDelete) && isOutput(toDelete)) {
						final List<PDGNode> toDeleteChildren = new LinkedList<PDGNode>();

						for (final PDGEdge out : pdg.outgoingEdgesOf(toDelete)) {
							if (out.kind == PDGEdge.Kind.PARAMETER_OUT && pdg.getId() != out.to.getPdgId()) {
								toDeleteChildren.add(out.to);
							}
						}

						pdg.removeAllVertices(toDeleteChildren);
					} else if (isActualParam(toDelete) && isInput(toDelete)) {
						final List<PDGNode> toDeleteChildren = new LinkedList<PDGNode>();

						for (final PDGEdge in : pdg.outgoingEdgesOf(toDelete)) {
							if (in.kind == PDGEdge.Kind.PARAMETER_IN && pdg.getId() != in.to.getPdgId()) {
								toDeleteChildren.add(in.to);
							}
						}

						pdg.removeAllVertices(toDeleteChildren);
					}

					pdg.removeVertex(toDelete);
				}

				deleteChildOrigNodes(pdg, child);
			}
		}
	}

	private static void connectChildren(final PDG caller, final TreeElem act, final PDG callee, final TreeElem form) {
		if (act.hasChildren()) {
			for (final TreeElem actChild : act.getChildren()) {
				final TreeElem formChild = form.findChild(actChild);

				if (isInput(actChild.node)) {
					caller.addVertex(formChild.node);
					caller.addEdge(actChild.node, formChild.node, PDGEdge.Kind.PARAMETER_IN);
				} else {
					callee.addVertex(actChild.node);
					callee.addEdge(formChild.node, actChild.node, PDGEdge.Kind.PARAMETER_OUT);
				}

				connectChildren(caller, actChild, callee, formChild);
			}
		}
	}

	private TreeRoots computeActualTrees(final PDG pdg, final PDGNode call) {
		final TreeRoots callRoots = TreeRoots.createCallRoots(pdg, call);

		for (final PDGNode root : callRoots.getRoots()) {
			final TreeElem rootTree = callRoots.getRootTree(root);
			addChildrenToTree(pdg, rootTree);
		}

		return callRoots;
	}

	private Map<PDG, TreeRoots> computeFormalTrees(final IProgressMonitor progress) throws CancelException {
		final Map<PDG, TreeRoots> trees = new HashMap<PDG, TreeRoots>();

		for (final PDG pdg : sdg.getAllPDGs()) {
			MonitorUtil.throwExceptionIfCanceled(progress);

			final TreeRoots pdgRoots = TreeRoots.createFormalRoots(pdg);

			for (final PDGNode root : pdgRoots.getRoots()) {
				final TreeElem rootTree = pdgRoots.getRootTree(root);
				addChildrenToTree(pdg, rootTree);
			}

			trees.put(pdg, pdgRoots);
		}

		return trees;
	}

	private static void addChildrenToTree(final PDG pdg, final TreeElem tree) {
		for (final PDGNode child : findDirectChildren(pdg, tree.orig)) {
			final TreeElem treeChild = tree.findOnPathToRoot(child);

			if (treeChild == null) {
				final PDGNode newChild = copyNode(pdg, child);
				copyDepsFromTo(pdg, child, newChild);
				pdg.addEdge(tree.node, newChild, PDGEdge.Kind.PARAM_STRUCT);
				final TreeElem newTreeChild = new TreeElem(tree, child, newChild);
				addChildrenToTree(pdg, newTreeChild);
			} else {
				pdg.addEdge(tree.node, treeChild.node, PDGEdge.Kind.PARAM_STRUCT);
			}
		}
	}

	private static final class TreeRoots {
		private final Map<PDGNode, TreeElem> roots = new HashMap<PDGNode, TreeElem>();;

		public static TreeRoots createFormalRoots(final PDG pdg) {
			final TreeRoots tr = new TreeRoots();

			final List<PDGNode> rootNodes = findFormRoots(pdg);
			for (final PDGNode r : rootNodes) {
				final TreeElem rootTree = new TreeElem(null, r, r);
				tr.roots.put(r, rootTree);
			}

			return tr;
		}

		public static TreeRoots createCallRoots(final PDG pdg, final PDGNode call) {
			final TreeRoots tr = new TreeRoots();

			final List<PDGNode> rootNodes = findCallRoots(pdg, call);
			for (final PDGNode r : rootNodes) {
				final TreeElem rootTree = new TreeElem(null, r, r);
				tr.roots.put(r, rootTree);
			}

			return tr;
		}

		private TreeRoots() {}

		public TreeElem getRootTree(final PDGNode n) {
			if (roots.containsKey(n)) {
				return roots.get(n);
			}

			return null;
		}

		public Set<PDGNode> getRoots() {
			return roots.keySet();
		}

	}

	private static final class TreeElem {
		public final TreeElem parent;
		public final PDGNode orig;
		public final PDGNode node;
		private List<TreeElem> children;

		private TreeElem(final TreeElem parent, final PDGNode orig, final PDGNode node) {
			this.parent = parent;
			this.orig = orig;
			this.node = node;

			if (parent != null) {
				parent.addChild(this);
			}
		}

		public boolean hasChildren() {
			return children != null && !children.isEmpty();
		}

		public TreeElem findChild(final TreeElem elem) {
			return getChild(elem.node.getBytecodeIndex(), elem.node.getBytecodeName(), isInput(elem.node));
		}

		public List<TreeElem> getChildren() {
			return children;
		}

		private void addChild(final TreeElem child) {
			if (children == null) {
				children = new LinkedList<TreeElem>();
			}

			children.add(child);
		}

		public TreeElem getChild(final int bcIndex, final String bcName, boolean isIn) {
			if (children != null) {
				for (final TreeElem ch : children) {
					final PDGNode chn = ch.node;
					if (chn.getBytecodeIndex() == bcIndex && bcName.equals(chn.getBytecodeName())
							&& ((isIn && isInput(chn)) || (!isIn && isOutput(chn)))) {
						return ch;
					}
				}
			}

			return null;
		}

		public TreeElem findOnPathToRoot(final PDGNode orig) {
			if (this.orig == orig) {
				return this;
			} else if (parent != null) {
				return parent.findOnPathToRoot(orig);
			}

			return null;
		}

//		public TreeElem findOnPathToRoot(final int bcIndex, final String bcName, boolean isIn) {
//			if (matches(bcIndex, bcName, isIn)) {
//				return this;
//			} else if (parent != null) {
//				return parent.findOnPathToRoot(bcIndex, bcName, isIn);
//			}
//
//			return null;
//		}
//
//		public boolean matches(final int bcIndex, final String bcName, boolean isIn) {
//			return node.getBytecodeIndex() == bcIndex && bcName.equals(node.getBytecodeName())
//					&& ((isIn && isInput(node)) || (!isIn && isOutput(node)));
//		}

	}

	private static boolean isInput(final PDGNode n) {
		switch (n.getKind()) {
		case ACTUAL_IN:
		case FORMAL_IN:
			return true;
		default: // nothing to do here
		}

		return false;
	}

	private static boolean isOutput(final PDGNode n) {
		switch (n.getKind()) {
		case ACTUAL_OUT:
		case FORMAL_OUT:
			return true;
		case EXIT:
			return n.getTypeRef() != TypeReference.Void;
		default: // nothing to do here
		}

		return false;
	}

	private static Map<PDGNode, PDGNode> findRootFormToAct(final PDG caller, final PDGNode call, final PDG callee) {
		final Map<PDGNode, PDGNode> form2act = new HashMap<PDGNode, PDGNode>();

		final List<PDGNode> acts = findAllParameterNodes(caller, call);
		final List<PDGNode> forms = findAllParameterNodes(callee, callee.entry);

		for (final PDGNode a : acts) {
			if (a.getKind() == PDGNode.Kind.ACTUAL_IN) {
				for (final PDGEdge e : caller.outgoingEdgesOf(a)) {
					if (e.kind == PDGEdge.Kind.PARAMETER_IN && e.to.getPdgId() == callee.getId()) {
						assert !form2act.containsKey(e.to);
						form2act.put(e.to, a);
					}
				}
			}
		}

		for (final PDGNode f : forms) {
			if (f.getKind() != PDGNode.Kind.FORMAL_IN) {
				for (final PDGEdge e : callee.outgoingEdgesOf(f)) {
					if (e.kind == PDGEdge.Kind.PARAMETER_OUT
							&& (e.to.getPdgId() == caller.getId() && acts.contains(e.to))) {
						assert !form2act.containsKey(f);
						form2act.put(f, e.to);
					}
				}
			}
		}


		return form2act;
	}

	private static List<PDGNode> findFormRoots(final PDG pdg) {
		final List<PDGNode> roots = new LinkedList<PDGNode>();

		for (final PDGEdge e : pdg.outgoingEdgesOf(pdg.entry)) {
			if (e.kind == PDGEdge.Kind.PARAM_STRUCT	&& isFormalParam(e.to)) {
				roots.add(e.to);
			}
		}

		return roots;
	}

	private static List<PDGNode> findCallRoots(final PDG pdg, final PDGNode call) {
		final List<PDGNode> roots = new LinkedList<PDGNode>();

		for (final PDGEdge e : pdg.outgoingEdgesOf(call)) {
			if (e.kind == PDGEdge.Kind.PARAM_STRUCT	&& isActualParam(e.to)) {
				roots.add(e.to);
			}
		}

		return roots;
	}

	// Copy all except parameter-in/out and parameter structure edges.
	// And add node to control flow
	private static void copyDepsFromTo(final PDG pdg, final PDGNode copyFrom, final PDGNode copyTo) {
		final List<PDGEdge> copyIncoming = new LinkedList<PDGEdge>();
		for (final PDGEdge in : pdg.incomingEdgesOf(copyFrom)) {
			switch (in.kind) {
			case CONTROL_FLOW:
			case CONTROL_FLOW_EXC:
			case PARAMETER_IN:
			case PARAMETER_OUT:
			case PARAM_STRUCT:
				break;
			default:
				copyIncoming.add(in);
				break;
			}
		}

		for (final PDGEdge inCopy : copyIncoming) {
			pdg.addEdge(inCopy.from, copyTo, inCopy.kind);
		}

		final List<PDGEdge> copyOutgoing = new LinkedList<PDGEdge>();
		for (final PDGEdge out : pdg.outgoingEdgesOf(copyFrom)) {
			switch (out.kind) {
			case CONTROL_FLOW:
			case CONTROL_FLOW_EXC:
			case PARAMETER_IN:
			case PARAMETER_OUT:
			case PARAM_STRUCT:
				break;
			default:
				copyOutgoing.add(out);
				break;
			}
		}

		for (final PDGEdge outCopy : copyOutgoing) {
			pdg.addEdge(copyTo, outCopy.to, outCopy.kind);
		}
	}

	private static List<PDGNode> findDirectChildren(final PDG pdg, final PDGNode n) {
		final List<PDGNode> children = new LinkedList<PDGNode>();

		for (final PDGEdge e : pdg.outgoingEdgesOf(n)) {
			if (e.kind == PDGEdge.Kind.PARAM_STRUCT) {
				children.add(e.to);
			}
		}

		return children;
	}

	private static List<PDGNode> findAllParameterNodes(final PDG pdg, final PDGNode n) {
		final List<PDGNode> params = new LinkedList<PDGNode>();

		for (final PDGEdge e : pdg.outgoingEdgesOf(n)) {
			if (e.kind == PDGEdge.Kind.CONTROL_DEP_EXPR && (isActualParam(e.to) || isFormalParam(e.to))) {
				params.add(e.to);
			}
		}

		return params;
	}

	private static boolean isActualParam(final PDGNode n) {
		switch (n.getKind()) {
		case ACTUAL_IN:
		case ACTUAL_OUT:
			return true;
		default: // nothing to do here
		}

		return false;
	}

	private static boolean isFormalParam(final PDGNode n) {
		switch (n.getKind()) {
		case FORMAL_IN:
		case FORMAL_OUT:
			return true;
		case EXIT:
			return n.getTypeRef() != TypeReference.Void;
		default: // nothing to do here
		}

		return false;
	}

	private static PDGNode copyNode(final PDG pdg, final PDGNode toCopy) {
		final PDGNode copy = pdg.createNode(toCopy.getLabel(), toCopy.getKind(), toCopy.getTypeRef(), toCopy.getLocalDefNames());
		copy.setBytecodeIndex(toCopy.getBytecodeIndex());
		copy.setBytecodeName(toCopy.getBytecodeName());
		copy.setDebug(toCopy.getDebug());
		copy.setSourceLocation(toCopy.getSourceLocation());
		copy.setParameterField(toCopy.getParameterField());

		return copy;
	}

}
