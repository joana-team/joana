/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath.nodes;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.graph.impl.SlowNumberedNodeManager;
import com.ibm.wala.util.graph.impl.SparseNumberedEdgeManager;

import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGField;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.accesspath.AP;
import edu.kit.joana.wala.core.accesspath.AP.RootNode;

public final class APGraph extends AbstractNumberedGraph<APNode> {

	public static APGraph create(final PDG pdg) {
		final APGraph apg = new APGraph(pdg);

		apg.initialize();

		return apg;
	}

	public static final class APEdge {
		public final APNode from;
		public final APNode to;

		private APEdge(final APNode from, final APNode to) {
			if (from == null || to == null) {
				throw new IllegalArgumentException("Arguments should not be null: " + from + ", " + to);
			}

			this.from = from;
			this.to = to;
		}

		public int hashCode() {
			return from.hashCode() * 17 + to.hashCode();
		}

		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj instanceof APEdge) {
				final APEdge other = (APEdge) obj;

				return from.equals(other.from) && to.equals(other.to);
			}

			return false;
		}

		public String toString() {
			return from.toString() + "->" + to.toString();
		}

	}

	public static List<APEdge> findAliasEdges(final APGraph g) {
		final List<APEdge> alias = new LinkedList<APGraph.APEdge>();

		for (final APNode apFrom : g) {
			final PDGNode from = apFrom.node;
			for (final PDGEdge e : g.pdg.outgoingEdgesOf(from)) {
				switch (e.kind) {
				case DATA_ALIAS:
				case DATA_HEAP:
					final PDGNode to = e.to;
					final APNode apTo = g.pdg2ap.get(to);
					assert (apTo != null) : "Could not find AP node for " + to.getId();
					alias.add(new APEdge(apFrom, apTo));
					break;
				default: // nothing to do here
				}
			}
		}

		return alias;
	}

	private final SlowNumberedNodeManager<APNode> nodeManager;
	private final SparseNumberedEdgeManager<APNode> edgeManager;
	private final Map<PDGNode, APNode> pdg2ap = new HashMap<PDGNode, APNode>();
	private final Map<RootNode, PDGNode> root2pdg = new HashMap<RootNode, PDGNode>();
	private final Map<APNode, AP> initialValues = new HashMap<APNode, AP>();
	private final PDG pdg;
	private APEntryNode entry;

	private APGraph(final PDG pdg) {
		this.nodeManager = new SlowNumberedNodeManager<APNode>();
		this.edgeManager = new SparseNumberedEdgeManager<APNode>(nodeManager);
		this.pdg = pdg;
	}

	@Override
	protected NumberedNodeManager<APNode> getNodeManager() {
		return nodeManager;
	}

	@Override
	protected NumberedEdgeManager<APNode> getEdgeManager() {
		return edgeManager;
	}

	private void initialize() {
		createNodes();
		createEdges();
	}

	/**
	 * Saves the initial value of root nodes. This is used to identify which root path nodes
	 * should be translated when accesspaths are propagated from callee to caller.
	 */
	private void addInitialValue(final APNode n, final AP init) {
		initialValues.put(n, init);
	}

//	private void addInitialValue(final APParamNode n) {
//		final AP ap = n.getOutgoingPaths().next();
//		initialValues.put(n, ap);
//
//		if (n.hasChildren()) {
//			for (final APParamNode child : n.getChildren()) {
//				addInitialValue(child);
//			}
//		}
//	}

	private void createNodes() {
		assert entry == null;

		for (final PDGField f : pdg.getFieldReads()) {
			if (!f.field.isStatic()) {
				final SSAInstruction instr = pdg.getInstruction(f.node);
				{
				final APFieldNode apn = APFieldNode.createFieldGet(instr.iindex, f, f.base);
				pdg2ap.put(f.base, apn);
				addNode(apn);
				} {
				final APNormNode apfield = new APNormNode(instr.iindex, f.accfield);
				pdg2ap.put(f.accfield, apfield);
				addNode(apfield);
				}
				final APNormNode apread = new APNormNode(instr.iindex, f.node);
				pdg2ap.put(f.node, apread);
				addNode(apread);

				if (f.field.isArray()) {
					final APNormNode apindex = new APNormNode(instr.iindex, f.index);
					pdg2ap.put(f.index, apindex);
					addNode(apindex);
				}
			} else {
				final SSAInstruction instr = pdg.getInstruction(f.node);
				if (instr != null) {
					final APNormNode apn = new APNormNode(instr.iindex, f.node);
					pdg2ap.put(f.node, apn);
					addNode(apn);

					final APNormNode apfield = new APNormNode(instr.iindex, f.accfield);
					pdg2ap.put(f.node, apfield);
					addNode(apfield);
				}
			}
		}

		for (final PDGField f : pdg.getFieldWrites()) {
			if (!f.field.isStatic()) {
				final SSAInstruction instr = pdg.getInstruction(f.node);
				{
				final APFieldNode apn = APFieldNode.createFieldGet(instr.iindex, f, f.base);
				pdg2ap.put(f.base, apn);
				addNode(apn);
				} {
				final APNormNode apfield = new APNormNode(instr.iindex, f.accfield);
				pdg2ap.put(f.accfield, apfield);
				addNode(apfield);
				}
				final APNormNode apwrite = new APNormNode(instr.iindex, f.node);
				pdg2ap.put(f.node, apwrite);
				addNode(apwrite);

				if (f.field.isArray()) {
					final APNormNode apindex = new APNormNode(instr.iindex, f.index);
					pdg2ap.put(f.index, apindex);
					addNode(apindex);
				}
			} else {
				final SSAInstruction instr = pdg.getInstruction(f.node);
				if (instr != null) {
					final APNormNode apn = new APNormNode(instr.iindex, f.node);
					pdg2ap.put(f.node, apn);
					addNode(apn);

					final APNormNode apfield = new APNormNode(instr.iindex, f.accfield);
					pdg2ap.put(f.node, apfield);
					addNode(apfield);
				}
			}
		}

		// create nodes
		for (final PDGNode n : pdg.vertexSet()) {
			if (n.getPdgId() != pdg.getId() || pdg2ap.containsKey(n)) {
				continue;
			}

			switch (n.getKind()) {
			case NORMAL:
			case EXPRESSION:
			case PREDICATE:
			case SYNCHRONIZATION:
			case PHI: {
				final SSAInstruction instr = pdg.getInstruction(n);
				if (instr != null) {
					final APNormNode apn = new APNormNode(instr.iindex, n);
					pdg2ap.put(n, apn);
					addNode(apn);
				}
			} break;
			case NEW: {
				final SSANewInstruction instr = (SSANewInstruction) pdg.getInstruction(n);
				final AP init = new AP(new AP.NewNode(pdg.cgNode, instr.getNewSite(), n.getId()));
				final APNewNode apn = APNewNode.create(instr.iindex, n, init);
				addInitialValue(apn, init);
				root2pdg.put(init.getRoot(), n);
				pdg2ap.put(n, apn);
				addNode(apn);
			} break;
			case HREAD:
			case HWRITE: {
				// added before
			} break;
			case CALL: {
				final APCallNode apn = createCallNode(n);
				pdg2ap.put(n, apn);
				addNode(apn);
			} break;
			case ENTRY: {
				final APEntryNode apn = createEntryNode(n);
				this.entry = apn;
				pdg2ap.put(n, apn);
				addNode(apn);
			} break;
			case FOLDED:
				throw new IllegalStateException("Cannot deal with folded nodes.");
			case ACTUAL_IN:
			case ACTUAL_OUT:
			case FORMAL_IN:
			case FORMAL_OUT:
			case EXIT:
				// these are added through creation of entry and call nodes.
				break;
			default:
				throw new IllegalStateException("Forgot to deal with node: " + n.getKind());
			}
		}

		assert entry != null;
	}

	private void createEdges() {
		// add initial data dependencies as edges.
		for (final PDGNode n : pdg.vertexSet()) {
			if (n.getPdgId() != pdg.getId()) {
				continue;
			}

			final APNode from = pdg2ap.get(n);

			if (from == null) {
				continue;
			}

			for (final PDGEdge e : pdg.outgoingEdgesOf(n)) {
				if (e.kind != PDGEdge.Kind.DATA_DEP) {
					continue;
				}

				final PDGNode tgt = e.to;

				if (tgt.getPdgId() != pdg.getId()) {
					continue;
				}

				final APNode to = pdg2ap.get(tgt);

				if (to == null) {
					continue;
				}

				addEdge(from, to);
			}
		}

		for (final PDGField f : pdg.getFieldReads()) {
			if (!f.field.isStatic()) {
				final APNode apbase = pdg2ap.get(f.base);
				final APNode apfield = pdg2ap.get(f.accfield);
				final APNode apget = pdg2ap.get(f.node);

				addEdge(apbase, apfield);
				removeEdge(apfield, apget);

				if (f.field.isArray()) {
					final APNode apindex = pdg2ap.get(f.index);

					removeEdge(apindex, apget);
				}
			}
		}

		for (final PDGField f : pdg.getFieldWrites()) {
			if (!f.field.isStatic()) {
				final APNode apbase = pdg2ap.get(f.base);
				final APNode apfield = pdg2ap.get(f.accfield);
				final APNode apget = pdg2ap.get(f.node);

				addEdge(apbase, apfield);
				removeEdge(apget, apfield);

				if (f.field.isArray()) {
					final APNode apindex = pdg2ap.get(f.index);

					removeEdge(apindex, apget);
				}
			}
		}
	}

	private APCallNode createCallNode(final PDGNode n) {
		final SSAInstruction instr = pdg.getInstruction(n);
		final int iindex = instr.iindex;
		final PDGNode[] pIn = pdg.getParamIn(n);
		final APActualParamNode[] apIn = new APActualParamNode[pIn.length];

		for (int i = 0; i < pIn.length; i++) {
			apIn[i] = APActualParamNode.createParamInRoot(iindex, pIn[i]);
			createActualChildren(apIn[i]);
		}

		final APActualParamNode apRet;
		{
			final PDGNode ret = pdg.getReturnOut(n);
			if (ret != null) {
				apRet = APActualParamNode.createParamOutRoot(iindex, ret);
				createActualChildren(apRet);
			} else {
				apRet = null;
			}
		}

		final APActualParamNode apExc;
		{
			final PDGNode exc = pdg.getExceptionOut(n);
			if (exc != null) {
				apExc = APActualParamNode.createParamOutRoot(iindex, exc);
				createActualChildren(apExc);
			} else {
				apExc = null;
			}
		}

		final APCallNode apCall = new APCallNode(iindex, n, apIn, apRet, apExc);

		final List<PDGField> sIns = pdg.getStaticIn(n);
		if (sIns != null) {
			for (final PDGField sIn : sIns) {
				if (!sIn.field.isPrimitiveType()) {
					final AP init = new AP(new AP.StaticParamNode(sIn.field.getField(), sIn.node.getId()));
					final APActualParamNode apSIn = APActualParamNode.createParamInStatic(iindex, sIn.node, sIn.field);
					createActualChildren(apSIn);
					apSIn.addPath(init);
					addInitialValue(apSIn, init);
					apCall.addParameterStaticIn(sIn.field.getField(), apSIn);
				}
			}
		}

		final List<PDGField> sOuts = pdg.getStaticOut(n);
		if (sOuts != null) {
			for (final PDGField sOut : sOuts) {
				if (!sOut.field.isPrimitiveType()) {
					final AP init = new AP(new AP.StaticParamNode(sOut.field.getField(), sOut.node.getId()));
					final APActualParamNode apSOut = APActualParamNode.createParamOutStatic(iindex, sOut.node, sOut.field);
					createActualChildren(apSOut);
					apSOut.addPath(init);
					addInitialValue(apSOut, init);
					apCall.addParameterStaticOut(sOut.field.getField(), apSOut);
				}
			}
		}

		return apCall;
	}

	private void createActualChildren(final APActualParamNode parent) {
		addNode(parent);
		pdg2ap.put(parent.node, parent);

		for (final PDGNode child : findDirectChildren(pdg, parent)) {
			assert child.getParameterField() != null;

			// check for back links
			if (pdg2ap.containsKey(child)) {
				continue;
			}

			final APActualParamNode apChild = (child.getKind() == PDGNode.Kind.ACTUAL_IN
					? APActualParamNode.createParamInChild(parent, parent.iindex, child, child.getParameterField())
					: APActualParamNode.createParamOutChild(parent, parent.iindex, child, child.getParameterField()));

			createActualChildren(apChild);
		}
	}

	private APEntryNode createEntryNode(final PDGNode n) {
		final int iindex = APNode.UNKNOWN_IINDEX;
		final PDGNode[] pIn = pdg.params;
		final APFormalParamNode[] apIn = new APFormalParamNode[pIn.length];

		for (int i = 0; i < pIn.length; i++) {
			final AP init = new AP(new AP.MethodParamNode(pdg.cgNode, i, pIn[i].getId()));
			apIn[i] = APFormalParamNode.createParamInRoot(iindex, pIn[i]);
			createFormalChildren(apIn[i]);
			apIn[i].addPath(init); // propagates path down to children
			addInitialValue(apIn[i], init);
			root2pdg.put(init.getRoot(), pIn[i]);
		}

		final APFormalParamNode apRet;
		{
			if (!pdg.isVoid()) {
				apRet = APFormalParamNode.createExit(iindex, pdg.exit);
				createFormalChildren(apRet);
			} else {
				apRet = null;
			}
		}

		final APFormalParamNode apExc;
		{
			final PDGNode exc = pdg.exception;
			if (exc != null) {
				apExc = APFormalParamNode.createException(iindex, exc);
				createFormalChildren(apExc);
			} else {
				apExc = null;
			}
		}

		final APEntryNode apEntry = new APEntryNode(iindex, n, apIn, apRet, apExc);

		for (final PDGField sIn : pdg.staticReads) {
			if (!sIn.field.isPrimitiveType()) {
				final AP init = new AP(new AP.StaticParamNode(sIn.field.getField(), sIn.node.getId()));
				final APFormalParamNode apSIn = APFormalParamNode.createParamInStatic(iindex, sIn.node, sIn.field);
				createFormalChildren(apSIn);
				apSIn.addPath(init);
				addInitialValue(apSIn, init);
				root2pdg.put(init.getRoot(), sIn.node);
				apEntry.addParameterStaticIn(sIn.field.getField(), apSIn);
			}
		}

		for (final PDGField sIn : pdg.staticInterprocReads) {
			if (!sIn.field.isPrimitiveType()) {
				final AP init = new AP(new AP.StaticParamNode(sIn.field.getField(), sIn.node.getId()));
				final APFormalParamNode apSIn = APFormalParamNode.createParamInStatic(iindex, sIn.node, sIn.field);
				createFormalChildren(apSIn);
				apSIn.addPath(init);
				addInitialValue(apSIn, init);
				root2pdg.put(init.getRoot(), sIn.node);
				apEntry.addParameterStaticIn(sIn.field.getField(), apSIn);
			}
		}

		for (final PDGField sOut : pdg.staticWrites) {
			if (!sOut.field.isPrimitiveType()) {
				final AP init = new AP(new AP.StaticParamNode(sOut.field.getField(), sOut.node.getId()));
				final APFormalParamNode apSOut = APFormalParamNode.createParamOutStatic(iindex, sOut.node, sOut.field);
				createFormalChildren(apSOut);
				apSOut.addPath(init);
				addInitialValue(apSOut, init);
				apEntry.addParameterStaticOut(sOut.field.getField(), apSOut);
			}
		}

		for (final PDGField sOut : pdg.staticInterprocWrites) {
			if (!sOut.field.isPrimitiveType()) {
				final AP init = new AP(new AP.StaticParamNode(sOut.field.getField(), sOut.node.getId()));
				final APFormalParamNode apSOut = APFormalParamNode.createParamOutStatic(iindex, sOut.node, sOut.field);
				createFormalChildren(apSOut);
				apSOut.addPath(init);
				addInitialValue(apSOut, init);
				apEntry.addParameterStaticOut(sOut.field.getField(), apSOut);
			}
		}

		return apEntry;
	}

	private void createFormalChildren(final APFormalParamNode parent) {
		addNode(parent);
		pdg2ap.put(parent.node, parent);

		for (final PDGNode child : findDirectChildren(pdg, parent)) {
			assert child.getParameterField() != null;

			// check for back links
			if (pdg2ap.containsKey(child)) {
				continue;
			}

			final APFormalParamNode apChild = (child.getKind() == PDGNode.Kind.FORMAL_IN
					? APFormalParamNode.createParamInChild(parent, parent.iindex, child, child.getParameterField())
					: APFormalParamNode.createParamOutChild(parent, parent.iindex, child, child.getParameterField()));

			createFormalChildren(apChild);
		}
	}

	private static List<PDGNode> findDirectChildren(final PDG pdg, final APParamNode p) {
		final List<PDGNode> children = new LinkedList<PDGNode>();

		for (final PDGEdge e : pdg.outgoingEdgesOf(p.node)) {
			if (e.kind == PDGEdge.Kind.PARAM_STRUCT) {
				children.add(e.to);
			}
		}

		return children;
	}

	public Collection<APCallNode> getCalls() {
		final List<APCallNode> calls = new LinkedList<APCallNode>();

		for (final PDGNode pdgCall : pdg.getCalls()) {
			final APNode apn = pdg2ap.get(pdgCall);
			assert apn != null;
			assert apn instanceof APCallNode;
			calls.add((APCallNode) apn);
		}

		return calls;
	}

	public APCallNode getCall(final PDGNode call) {
		if (call.getKind() != PDGNode.Kind.CALL) {
			throw new IllegalArgumentException("Not a call node: " + call.getKind() + "(" + call.getId() +")"
					+ call.getLabel());
		} else if (pdg.getId() != call.getPdgId()) {
			throw new IllegalArgumentException("Not part of this pdg " + pdg.toString() + ": " + call.getKind()
					+ "(" + call.getId() +")" + call.getLabel());
		}

		final APNode n = pdg2ap.get(call);
		assert n instanceof APCallNode;

		return (APCallNode) n;
	}

	public APEntryNode getEntry() {
		return entry;
	}

	public boolean hasInitialValue(final APNode n) {
		return initialValues.containsKey(n);
	}

	/**
	 * Creates a map from the root node of the initial value of the formal-in nodes of this method to the
	 * actual-in nodes of a call to this methods
	 * @param formIn2ActIn A map from the formal-in nodes of this methods to the actual-in nodes of the call
	 * to this method.
	 * @return A map from the initial access path root nodes to the actual-in nodes of the call.
	 */
	public Map<RootNode, APParamNode> createRoot2ActInMap(final Map<APParamNode, APParamNode> formIn2ActIn) {
		final Map<RootNode ,APParamNode> root2ap = new HashMap<AP.RootNode, APParamNode>();

		for (final Entry<APParamNode, APParamNode> e : formIn2ActIn.entrySet()) {
			if (initialValues.containsKey(e.getKey())) {
				final APParamNode formIn = e.getKey();
				final AP init = initialValues.get(formIn);
				final RootNode root = init.getRoot();
				final APParamNode actIn = e.getValue();
				root2ap.put(root, actIn);
			}
		}

		return root2ap;
	}

	public int getPDGNodeForRoot(final RootNode root) {
		return root.pdgNodeId;
	}
}
