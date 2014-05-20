/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params;

import java.io.IOException;
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
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphReachability;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.impl.SparseNumberedGraph;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.wala.core.DependenceGraph;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.util.WriteGraphToDot;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 * @deprecated does not work. just here as a source to copy code from
 */
public class HeapParams {

	private final SDGBuilder sdg;

	public static void compute(SDGBuilder sdg, IProgressMonitor progress) throws CancelException {
		HeapParams hp = new HeapParams(sdg);
		hp.run(progress);
	}

	private HeapParams(SDGBuilder sdg) {
		this.sdg = sdg;
	}

	private void run(IProgressMonitor progress) throws CancelException {
		addDataFlowForHeapFields(progress);
	}

	public static final String WRITE_ACCESS = "<w>";
	public static final String READ_WRITE_ACCESS = "<rw>";

	private void addDataFlowForHeapFields(IProgressMonitor progress) throws CancelException {
		DependenceGraph ddg = createInitialParamDependenceGraph();

		try {
			String file = "param_access";
			file = WriteGraphToDot.sanitizeFileName(file);
			WriteGraphToDot.write(ddg, file + ".ddg.dot");
		} catch (IOException exc) {
			exc.printStackTrace();
		}

		TIntObjectMap<Set<PDGNode>> folded = new TIntObjectHashMap<Set<PDGNode>>();
		// merge similar field accesses
		mergeFieldNodes(ddg, folded);

		try {
			String file = "param_access_basefld";
			file = WriteGraphToDot.sanitizeFileName(file);
			WriteGraphToDot.write(ddg, file + ".ddg.dot");
		} catch (IOException exc) {
			exc.printStackTrace();
		}

		createInterprocFields(ddg, folded, progress);
	}

	private void createInterprocFields(final DependenceGraph ddg, final TIntObjectMap<Set<PDGNode>> folded,
			final IProgressMonitor progress) throws CancelException {
		// create mapping: field -> int
		// this is not context sensitive, as theres only one entry per access instruction
		final OrdinalSetMapping<PDGNode> acc2id = createFieldAccessMapping(ddg);

		// this is kind of the points-to set for each access
		final TIntObjectMap<MutableIntSet> node2fields = createNode2FieldAccessMapping(acc2id, ddg, progress);

		for (final PDG pdg : sdg.getAllPDGs()) {
			// create fields per entry -> a new set for node candidates has to be made
			final Map<NodeCandidate, PDGNode> mod = new HashMap<NodeCandidate, PDGNode>();
			final Map<NodeCandidate, PDGNode> ref = new HashMap<NodeCandidate, PDGNode>();

			final Set<PDGNode> formInOut = new HashSet<PDGNode>();
			for (PDGEdge out : pdg.outgoingEdgesOf(pdg.entry)) {
				if (out.kind == PDGEdge.Kind.CONTROL_DEP_EXPR) {
					final PDGNode tgt = out.to;
					if ((tgt.getKind() == PDGNode.Kind.FORMAL_IN || tgt.getKind() == PDGNode.Kind.FORMAL_OUT
							|| (pdg.hasReturnValue() && tgt.getKind() == PDGNode.Kind.EXIT))
							&& ddg.containsVertex(tgt) && pdg.getId() == tgt.getPdgId()) {
						formInOut.add(tgt);
					}
				}
			}

			for (PDGNode fInOut : formInOut) {
				createFieldsFor(pdg, fInOut, node2fields, acc2id, mod, ref);
			}
		}

		for (final PDG pdg : sdg.getAllPDGs()) {
			for (final PDGNode call : pdg.getCalls()) {
				final PDGNode[] aInStd = pdg.getParamIn(call);
				final PDGNode aOutReturn = pdg.getReturnOut(call);
				final PDGNode aOutExc = pdg.getExceptionOut(call);

				for (final PDG callee : sdg.getPossibleTargets(call)) {
					final Set<PDGNode> done = new HashSet<PDGNode>();

					for (final PDGNode aIn : aInStd) {
						final List<PDGNode> formIns = new LinkedList<PDGNode>();

						for (final PDGEdge out : pdg.outgoingEdgesOf(aIn)) {
							if (out.kind == PDGEdge.Kind.PARAMETER_IN && out.to.getPdgId() == callee.getId()) {
								final PDGNode fIn = out.to;
								formIns.add(fIn);
							}
						}

						for (final PDGNode fIn : formIns) {
							createActualNodesFor(pdg, aIn, callee, fIn, node2fields, done);
						}
					}

					createActualNodesFor(pdg, aOutExc, callee, callee.exception, node2fields, done);

					if (aOutReturn != null) {
						createActualNodesFor(pdg, aOutReturn, callee, callee.exit, node2fields, done);
					}

					// create nodes for static fields
					for (final PDGEdge out : callee.outgoingEdgesOf(callee.entry)) {
						if (out.kind == PDGEdge.Kind.PARAM_STRUCT) {
							final PDGNode fNode = out.to;
							if (!done.contains(fNode)) {
								// create actual root node for fNode
								// create a matching parent
								final PDGNode actParent = createNewActChildFor(pdg, call, fNode, node2fields);

								createActualNodesFor(pdg, actParent, callee, fNode, node2fields, done);
							}
						}
					}
				}
			}

			// add dataflow for fields.
			addHeapDataFlowToPDG(pdg, node2fields, acc2id);
		}
	}

	private void addHeapDataFlowToPDG(final PDG pdg, final TIntObjectMap<MutableIntSet> node2fields,
			final OrdinalSetMapping<PDGNode> map) throws CancelException {
		final Graph<PDGNode> cfg = extractControlFlow(pdg);
		final Graph<PDGNode> invertcfg = GraphInverter.invert(cfg);
		final GraphReachability<PDGNode, PDGNode> reach =
			new GraphReachability<PDGNode, PDGNode>(invertcfg, new Filter<PDGNode>() {
				@Override
				public boolean accepts(PDGNode o) {
					return true;
				}
			});

		reach.solve(null);

		for (final PDGNode n : cfg) {
			final IntSet pts = node2fields.get(n.getId());
			if (pts != null && isRead(n)) {
				for (final PDGNode pred : reach.getReachableSet(n)) {
					final IntSet ptsPred = node2fields.get(pred.getId());
					if (ptsPred != null && pts.containsAny(ptsPred) && isWrite(pred)) {
						pdg.addEdge(pred, n, PDGEdge.Kind.DATA_HEAP);
					}
				}
			}
		}
	}

	private boolean isRead(final PDGNode n) {
		switch (n.getKind()) {
		case FORMAL_OUT:
		case ACTUAL_IN:
			return n.isFieldPtr();
		case HREAD:
			return true;
		case NORMAL:
			return n.getBytecodeIndex() < BytecodeLocation.UNDEFINED_POS_IN_BYTECODE &&
				!WRITE_ACCESS.equals(n.getDebug());
		default:
			return false;
		}
	}

	private boolean isWrite(final PDGNode n) {
		switch (n.getKind()) {
		case FORMAL_IN:
		case ACTUAL_OUT:
			return n.isFieldPtr();
		case HWRITE:
			return true;
		case NORMAL:
			return n.getBytecodeIndex() < BytecodeLocation.UNDEFINED_POS_IN_BYTECODE &&
				(WRITE_ACCESS.equals(n.getDebug()) || READ_WRITE_ACCESS.equals(n.getDebug()));
		default:
			return false;
		}
	}

	private static Graph<PDGNode> extractControlFlow(PDG pdg) {
		Graph<PDGNode> cfg = new SparseNumberedGraph<PDGNode>();
		for (PDGNode node : pdg.vertexSet()) {
			if (node.getPdgId() == pdg.getId()) {
				cfg.addNode(node);
			}
		}

		for (PDGNode node : pdg.vertexSet()) {
			if (node.getPdgId() == pdg.getId()) {
				for (PDGEdge edge : pdg.outgoingEdgesOf(node)) {
					if (edge.kind.isFlow() && edge.to.getPdgId() == pdg.getId()) {
						cfg.addEdge(node, edge.to);
					}
				}
			}
		}

		return cfg;
	}

	private void createActualNodesFor(final PDG caller, PDGNode actParent, final PDG callee, final PDGNode formParent,
			final TIntObjectMap<MutableIntSet> node2fields,	final Set<PDGNode> done) {

		if (!done.contains(formParent)) {
			done.add(formParent);

			for (final PDGEdge out : callee.outgoingEdgesOf(formParent)) {
				if (out.kind == PDGEdge.Kind.PARAM_STRUCT && !done.contains(out.to)) {
					// add child to actRoot for out.to
					final PDGNode formChild = out.to;
					final PDGNode actChild = createNewActChildFor(caller, actParent, formChild, node2fields);

					createActualNodesFor(caller, actChild, callee, formChild, node2fields, done);
				}
			}
		}
	}

	private PDGNode createNewActChildFor(final PDG caller, final PDGNode actParent, final PDGNode formChild,
			final TIntObjectMap<MutableIntSet> node2fields) {
		final PDGNode child;

		final String name = formChild.getLabel();
		final String bcFieldName = formChild.getBytecodeName();
		final int bcIndex = formChild.getBytecodeIndex();
		final TypeReference bcType = formChild.getTypeRef();

		if (formChild.getKind() == PDGNode.Kind.FORMAL_IN) {
			final PDGNode exists = caller.searchInputField(actParent, bcFieldName);
			if (exists == null) {
				child = caller.addInputFieldChildTo(actParent, name, bcFieldName, bcIndex, bcType);
			} else {
				child = exists;
			}
		} else if (formChild.getKind() == PDGNode.Kind.EXIT || formChild.getKind() == PDGNode.Kind.FORMAL_OUT) {
			final PDGNode exists = caller.searchOutputField(actParent, bcFieldName);
			if (exists == null) {
				child = caller.addOutputFieldChildTo(actParent, name, bcFieldName, bcIndex, bcType);
			} else {
				child = exists;
			}
		} else {
			throw new IllegalStateException("Parent node of unexcpected type: " + formChild.toString());
		}

		final IntSet formbv = node2fields.get(formChild.getId());
		MutableIntSet chbv = node2fields.get(child.getId());
		if (chbv == null) {
			chbv = new BitVectorIntSet();
			node2fields.put(child.getId(), chbv);
		}
		if (formbv != null) {
			// it may be null for static field root nodes
			chbv.addAll(formbv);
		}

		return child;
	}

	/**
	 * Creates a mapping for each node of the ddg to a set (bitvector) of field accesses.
	 * @param acc2id Mapping for the bitvector.
	 * @param ddg The data dependence graph.
	 * @param progress Progress monitor.
	 * @return A mapping for each node in the ddg to the direclty referenced fields through this node.
	 * @throws CancelException
	 */
	private static TIntObjectMap<MutableIntSet> createNode2FieldAccessMapping(final OrdinalSetMapping<PDGNode> acc2id,
			final DependenceGraph ddg, final IProgressMonitor progress) throws CancelException {
		final TIntObjectMap<MutableIntSet> node2fields = new TIntObjectHashMap<MutableIntSet>();

		// store direct field successors
		for (final PDGNode n : ddg.vertexSet()) {
			final MutableIntSet fields = new BitVectorIntSet();
			node2fields.put(n.getId(), fields);

			for (final PDGEdge e : ddg.outgoingEdgesOf(n)) {
				if (e.kind == PDGEdge.Kind.PARAM_STRUCT) {
					final int id = acc2id.getMappedIndex(e.to);
					assert id >= 0;

					fields.add(id);
				}
			}
		}

		boolean changed = true;
		// does not terminate....
		while (changed) {
			changed = false;
			MonitorUtil.throwExceptionIfCanceled(progress);

			// merge fields on data dep (intraproc)
			for (final PDGNode n1 : ddg.vertexSet()) {
				for (final PDGEdge e : ddg.outgoingEdgesOf(n1)) {
					if (e.kind.isData() && e.kind != PDGEdge.Kind.PARAMETER_IN &&  e.kind != PDGEdge.Kind.PARAMETER_OUT) {
						final PDGNode n2 = e.to;
						final MutableIntSet s1 = node2fields.get(n1.getId());
						final MutableIntSet s2 = node2fields.get(n2.getId());
						changed |= s1.addAll(s2);
						changed |= s2.addAll(s1);
					}
				}
			}

			// propagate fields through callsites (interproc)
			for (final PDGNode n1 : ddg.vertexSet()) {
				for (final PDGEdge e : ddg.outgoingEdgesOf(n1)) {
					// if nodes are not copied (like atm) theres no contextsensitivity.
					/*
					 * A a;
					 * A b;
					 * foo(a);		// these calls refer to the same field access
					 * foo(b);		// and can only be distinguished by the callsite
					 *
					 * foo(A p) {
					 * 	p.f = 3;
					 * }
					 *
					 */
					if (e.kind == PDGEdge.Kind.PARAMETER_IN) {
						// copy nodes => call + field
						// copy from form-in to act-in
						final PDGNode n2 = e.to;
						final MutableIntSet s1 = node2fields.get(n1.getId());
						final MutableIntSet s2 = node2fields.get(n2.getId());
						changed |= s1.addAll(s2);
					} else if (e.kind == PDGEdge.Kind.PARAMETER_OUT) {
						// copy nodes => call + field
						// copy from form-out to act-out
						final PDGNode n2 = e.to;
						final MutableIntSet s1 = node2fields.get(n1.getId());
						final MutableIntSet s2 = node2fields.get(n2.getId());
						changed |= s2.addAll(s1);
					}
				}
			}
		}

		return node2fields;
	}

	private static class NodeCandidate {
		private final String name;
		private final IntSet refs;

		private NodeCandidate(String name, IntSet refs) {
			this.name = name;
			this.refs = refs;
		}

		public int hashCode() {
			return name.hashCode() + (4711 * refs.size());
		}

		public boolean equals(Object obj) {
			if (obj instanceof NodeCandidate) {
				NodeCandidate nc = (NodeCandidate) obj;
				return name.equals(nc.name) && refs.sameValue(nc.refs);
			}

			return false;
		}
	}

	/**
	 * Creates all field nodes for a given method parameter.
	 * @param pdg The PDG of the method the parameter belongs to.
	 * @param n The root node of the parameter.
	 * @param node2fields Maps each node (parameters and fields) to a set of directly referenced fields.
	 * @param map Maps each value of a bitvector/intset to a PDGNode.
	 * @param mod Maps a node candidate (field name x access nodes) of a modify operation to a parameter
	 * node that was created for this node. This is to prevent the creation of multiple parameter nodes
	 * for the same candidate.
	 * @param ref Maps a node candidate (field name x access nodes) of a read operation to a parameter
	 * node that was created for this node. This is to prevent the creation of multiple parameter nodes
	 * for the same candidate.
	 */
	private void createFieldsFor(final PDG pdg, final PDGNode n, final TIntObjectMap<MutableIntSet> node2fields,
			final OrdinalSetMapping<PDGNode> map, final Map<NodeCandidate, PDGNode> mod, final Map<NodeCandidate, PDGNode> ref) {
		final MutableIntSet bvfields = node2fields.get(n.getId());
		final OrdinalSet<PDGNode> fields = new OrdinalSet<PDGNode>(bvfields, map);

		// group fields by name
		final Map<String, MutableIntSet> fname2refs = new HashMap<String, MutableIntSet>();
		for (final PDGNode f : fields) {
			final String fname = f.getBytecodeName();
			MutableIntSet bv = fname2refs.get(fname);
			if (bv == null) {
				bv = new BitVectorIntSet();
				fname2refs.put(fname, bv);
			}

			final int fid = map.getMappedIndex(f);
			assert fid >= 0;
			bv.add(fid);
		}

		for (final Map.Entry<String, MutableIntSet> entry : fname2refs.entrySet()) {
			// create node as child of n
			final NodeCandidate cand = new NodeCandidate(entry.getKey(), entry.getValue());

			final List<PDGNode> children = addFieldNodesForAccess(pdg, n, cand, map, mod, ref);

			for (PDGNode field : children) {
				MutableIntSet bv = node2fields.get(field.getId());
				if (bv == null) {
					bv = new BitVectorIntSet();
					bv.addAll(entry.getValue());
					node2fields.put(field.getId(), bv);
				} else {
					bv.addAll(entry.getValue());
				}

				addChildFieldsTo(pdg, field, node2fields, map, mod, ref);
			}
		}
	}

	private void addChildFieldsTo(final PDG pdg, final PDGNode field, final TIntObjectMap<MutableIntSet> param2field,
			final OrdinalSetMapping<PDGNode> map, final Map<NodeCandidate, PDGNode> mod, final Map<NodeCandidate, PDGNode> ref) {
		final IntSet refs = param2field.get(field.getId());
		final OrdinalSet<PDGNode> refNodes = new OrdinalSet<PDGNode>(refs, map);

		// collect all reached fields
		final MutableIntSet referredFields = new BitVectorIntSet();
		for (final PDGNode refn : refNodes) {
			final IntSet refField = param2field.get(refn.getId());

			referredFields.addAll(refField);
		}

		// do the fieldname -> nodes mapping
		final OrdinalSet<PDGNode> fields = new OrdinalSet<PDGNode>(referredFields, map);
		// group fields by name
		final Map<String, MutableIntSet> fname2refs = new HashMap<String, MutableIntSet>();
		for (final PDGNode f : fields) {
			final String fname = f.getBytecodeName();
			MutableIntSet bv = fname2refs.get(fname);
			if (bv == null) {
				bv = new BitVectorIntSet();
				fname2refs.put(fname, bv);
			}

			final int fid = map.getMappedIndex(f);
			assert fid >= 0;
			bv.add(fid);
		}

		for (final Map.Entry<String, MutableIntSet> entry : fname2refs.entrySet()) {
			// create node as child of n
			final NodeCandidate cand = new NodeCandidate(entry.getKey(), entry.getValue());

			final List<PDGNode> children = addFieldNodesForAccess(pdg, field, cand, map, mod, ref);

			for (PDGNode child : children) {
				MutableIntSet bv = param2field.get(child.getId());
				if (bv == null) {
					bv = new BitVectorIntSet();
					bv.addAll(entry.getValue());
					param2field.put(child.getId(), bv);
				} else {
					bv.addAll(entry.getValue());
				}

				addChildFieldsTo(pdg, child, param2field, map, mod, ref);
			}
		}
	}

	private List<PDGNode> addFieldNodesForAccess(final PDG pdg, final PDGNode fIn, final NodeCandidate cand,
			final OrdinalSetMapping<PDGNode> map, final Map<NodeCandidate, PDGNode> mod, final Map<NodeCandidate, PDGNode> ref) {
		String name = null;
		int bcIndex = 0;
		TypeReference bcType = null;
		boolean isRead = false;
		boolean isWrite = false;

		OrdinalSet<PDGNode> accesses = new OrdinalSet<PDGNode>(cand.refs, map);

		for (final PDGNode n : accesses) {
			name = n.getLabel();
			bcIndex = n.getBytecodeIndex();
			bcType = n.getTypeRef();

			final String dbg = n.getDebug();
			if (dbg == null || dbg.isEmpty()) {
				isRead = true;
			} else if (dbg.equals(WRITE_ACCESS)) {
				isWrite = true;
			} else if (dbg.equals(READ_WRITE_ACCESS)) {
				isWrite = true;
				isRead = true;
			} else {
				isRead = true;
			}
		}

		final List<PDGNode> result = new LinkedList<PDGNode>();

		switch (fIn.getKind()) {
		case FORMAL_IN:
		case ACTUAL_IN: {
			if (isRead) {
				if (!ref.containsKey(cand)) {
					final PDGNode nf = pdg.addInputFieldChildTo(fIn, name, cand.name, bcIndex, bcType);
					result.add(nf);
					ref.put(cand, nf);
				} else {
					// get pdg node for candidate and add param struct reference
					final PDGNode of = ref.get(cand);
					pdg.addEdge(fIn, of, PDGEdge.Kind.PARAM_STRUCT);
				}
			}

			if (isWrite) {
				if (!mod.containsKey(cand)) {
					final PDGNode nf = pdg.addOutputFieldChildTo(fIn, name, cand.name, bcIndex, bcType);
					nf.setDebug(WRITE_ACCESS);
					result.add(nf);
					mod.put(cand, nf);
				} else {
					// get pdg node for candidate and add param struct reference
					final PDGNode of = mod.get(cand);
					pdg.addEdge(fIn, of, PDGEdge.Kind.PARAM_STRUCT);
				}
			}
		} break;
		case EXIT: {
			if (isRead && !isWrite) {
				if (!mod.containsKey(cand)) {
					final PDGNode nf = pdg.addOutputFieldChildTo(fIn, name, cand.name, bcIndex, bcType);
					result.add(nf);
					mod.put(cand, nf);
				} else {
					final PDGNode of = mod.get(cand);
					if (WRITE_ACCESS.equals(of.getDebug())) {
						of.setDebug(READ_WRITE_ACCESS);
					}
					pdg.addEdge(fIn, of, PDGEdge.Kind.PARAM_STRUCT);
				}
			} else if (isWrite) {
				if (!mod.containsKey(cand)) {
					final PDGNode nf = pdg.addOutputFieldChildTo(fIn, name, cand.name, bcIndex, bcType);
					nf.setDebug((isRead ? READ_WRITE_ACCESS : WRITE_ACCESS));
					result.add(nf);
					mod.put(cand, nf);
				} else {
					final PDGNode of = mod.get(cand);
					if (!WRITE_ACCESS.equals(of.getDebug())) {
						of.setDebug(READ_WRITE_ACCESS);
					}
					pdg.addEdge(fIn, of, PDGEdge.Kind.PARAM_STRUCT);
				}
			}
		} break;
		case ACTUAL_OUT:
		case FORMAL_OUT: {
			if (isWrite || isRead) {
				if (!mod.containsKey(cand)) {
					final PDGNode nf = pdg.addOutputFieldChildTo(fIn, name, cand.name, bcIndex, bcType);
					if (isWrite) {
						nf.setDebug((isRead ? READ_WRITE_ACCESS : WRITE_ACCESS));
					}
					result.add(nf);
					mod.put(cand, nf);
				} else {
					final PDGNode of = mod.get(cand);
					if (isRead || !WRITE_ACCESS.equals(of.getDebug())) {
						of.setDebug(READ_WRITE_ACCESS);
					}
					pdg.addEdge(fIn, of, PDGEdge.Kind.PARAM_STRUCT);
				}
			}
		} break;
		default:
			throw new IllegalArgumentException("Can not add a child to this node: " + fIn.toString());
		}

		return result;
	}

	private OrdinalSetMapping<PDGNode> createFieldAccessMapping(DependenceGraph ddg) {
		Set<PDGNode> access = new HashSet<PDGNode>();
		for (PDGNode n : ddg.vertexSet()) {
			if (n.isFieldPtr()) {
				access.add(n);
			}
		}

		PDGNode[] arr = new PDGNode[access.size()];
		access.toArray(arr);
		OrdinalSetMapping<PDGNode> mapping = new ObjectArrayMapping<PDGNode>(arr);

		return mapping;
	}

	/**
	 * Merge intraprocedural similar field access into a single access node.
	 * <pre>
	 * obj.f = 4;
	 * obj.f = 5;
	 * x = obj.f;
	 * arr[3] = 2;
	 * arr[5] = 6;
	 * </pre>
	 * This does not merge interprocedural accesses, because it would destroy context-sensitivity.
	 * @param ddg The dependencegraph containing the field access structure nodes
	 * @param folded A map that stores the mapping of a folded node to its members.
	 */
	private void mergeFieldNodes(DependenceGraph ddg, TIntObjectMap<Set<PDGNode>> folded) {
		Set<PDGNode> fields = new HashSet<PDGNode>();
		for (PDGNode field : ddg.vertexSet()) {
			if (field.isFieldPtr()) {
				fields.add(field);
			}
		}

		while (!fields.isEmpty()) {
			PDGNode field = fields.iterator().next();
			fields.remove(field);

			Set<PDGNode> toCheck = new HashSet<PDGNode>();
			for (PDGEdge in : ddg.incomingEdgesOf(field)) {
				if (in.kind == PDGEdge.Kind.PARAM_STRUCT) {
					for (PDGEdge out : ddg.outgoingEdgesOf(in.from)) {
						if (out.kind == PDGEdge.Kind.PARAM_STRUCT && out.to != field && out.to.isFieldPtr()
								&& out.to.getBytecodeName().equals(field.getBytecodeName())) {
							toCheck.add(out.to);
						}
					}
				}
			}

			List<PDGNode> toMerge = new LinkedList<PDGNode>();
			for (PDGNode chk : toCheck) {
				if (mergeFieldOk(ddg, field, chk)) {
					toMerge.add(chk);
				}
			}

			if (!toMerge.isEmpty()) {
				fields.removeAll(toMerge);
				toMerge.add(field);
				merge(ddg, folded, toMerge);
			}
		}
	}

	private void merge(DependenceGraph ddg, TIntObjectMap<Set<PDGNode>> folded, List<PDGNode> toMerge) {
		Set<PDGNode> fls = new HashSet<PDGNode>();

		boolean read = false;
		boolean write = false;
		for (PDGNode n : toMerge) {
			if (n.getKind() == PDGNode.Kind.FOLDED) {
				for (PDGNode nf : folded.get(n.getId())) {
					fls.add(nf);

					String dbg = nf.getDebug();
					if (dbg == WRITE_ACCESS) {
						write = true;
					} else if (dbg == READ_WRITE_ACCESS) {
						write = true;
						read = true;
					} else {
						read = true;
					}
				}

				folded.remove(n.getId());
			} else {
				String dbg = n.getDebug();

				if (dbg == WRITE_ACCESS) {
					write = true;
				} else if (dbg == READ_WRITE_ACCESS) {
					write = true;
					read = true;
				} else {
					read = true;
				}

				fls.add(n);
			}
		}

		final PDGNode first = fls.iterator().next();
		String label = first.isFieldPtr() ? "fold " + first.getBytecodeName() : "fold base";

		PDGNode fld = new PDGNode(sdg.getNextNodeId(), SDGBuilder.NO_PDG_ID, label, PDGNode.Kind.FOLDED, PDGNode.DEFAULT_TYPE);
		fld.setBytecodeIndex(first.getBytecodeIndex());
		fld.setBytecodeName(first.getBytecodeName());
		if (read && write) {
			fld.setDebug(READ_WRITE_ACCESS);
		} else if (write) {
			fld.setDebug(WRITE_ACCESS);
		}

		ddg.addVertex(fld);

		folded.put(fld.getId(), fls);

		List<PDGEdge> toRemove = new LinkedList<PDGEdge>();

		for (PDGNode n : toMerge) {
			for (PDGEdge in : ddg.incomingEdgesOf(n)) {
				if (!toMerge.contains(in.from)) {
					ddg.addEdge(in.from, fld, in.kind);
				}

				toRemove.add(in);
			}

			for (PDGEdge out : ddg.outgoingEdgesOf(n)) {
				if (!toMerge.contains(out.to)) {
					ddg.addEdge(fld, out.to, out.kind);
				}

				toRemove.add(out);
			}
		}

		ddg.removeAllEdges(toRemove);
		ddg.removeAllVertices(toMerge);
	}

	private static boolean mergeFieldOk(DependenceGraph ddg, PDGNode n1, PDGNode n2) {
		if (!n1.getBytecodeName().equals(n2.getBytecodeName())) {
			return false;
		}

		BitVector pred1 = new BitVector();
		for (PDGEdge in1 : ddg.incomingEdgesOf(n1)) {
			if (in1.kind == PDGEdge.Kind.PARAM_STRUCT && in1.from != n1) {
				pred1.set(in1.from.getId());
			}
		}

		BitVector pred2 = new BitVector();
		for (PDGEdge in2 : ddg.incomingEdgesOf(n2)) {
			if (in2.kind == PDGEdge.Kind.PARAM_STRUCT && in2.from != n2) {
				pred2.set(in2.from.getId());
			}
		}

		pred1.xor(pred2);

		return pred1.isZero();
	}

	private DependenceGraph createInitialParamDependenceGraph() {
		DependenceGraph ddg = new DependenceGraph();

		for (final PDG pdg : sdg.getAllPDGs()) {
			// compute intraproc fields
			for (PDGNode node : pdg.vertexSet()) {
				ddg.addVertex(node);
			}

			List<PDGNode> toRemove = new LinkedList<PDGNode>();
			for (PDGNode node : pdg.vertexSet()) {
				final boolean isBaseOrField = node.isBasePtr() || node.isObjectField() || node.isRootParam() || node.isStaticField();

				for (PDGEdge out : pdg.outgoingEdgesOf(node)) {
					if (out.kind.isData() && !node.isBasePtr()) {
						switch (out.kind) {
						case PARAMETER_IN:
						case PARAMETER_OUT:
							ddg.addEdge(node, out.to, out.kind);
							break;
						default:
							ddg.addEdge(node, out.to, PDGEdge.Kind.DATA_DEP);
							break;
						}
					}

					if (isBaseOrField && out.kind == PDGEdge.Kind.PARAM_STRUCT) {
						ddg.addEdge(node, out.to, PDGEdge.Kind.PARAM_STRUCT);
					}
				}

				switch (node.getKind()) {
				case ACTUAL_IN:
				case ACTUAL_OUT:
				case FORMAL_IN:
				case FORMAL_OUT:
				case NEW:
					break;
				case EXIT:
					if (pdg.getMethod().getReturnType() == TypeReference.Void) {
						toRemove.add(node);
					}
					break;
				case NORMAL:
//					if (!node.isBasePtr() && !node.isFieldPtr()) {
					if (!node.isFieldPtr()) {
						toRemove.add(node);
					}
					break;
				default:
					toRemove.add(node);
				}

				if (node.getKind() == PDGNode.Kind.ACTUAL_IN  || node.getKind() == PDGNode.Kind.ACTUAL_OUT) {
					for (PDGEdge in : pdg.incomingEdgesOf(node)) {
						if (in.kind.isControl() && in.from.getKind() == PDGNode.Kind.CALL) {
							node.setDebug("cl" + in.from.getId());
							break;
						}
					}
				} else if (node.isFieldPtr()) {
					// mark field nodes read & write

					for (PDGEdge in : pdg.incomingEdgesOf(node)) {
						if (in.kind == PDGEdge.Kind.CONTROL_DEP_EXPR) {
							switch (in.from.getKind()) {
							case HREAD:
								break;
							case HWRITE:
								node.setDebug(WRITE_ACCESS);
								break;
							default:
								throw new IllegalStateException("A field node is only connected to a heap read or write.");
							}
						}
					}
				}

			}

			for (PDGNode node : toRemove) {
				ddg.removeNode(node);
			}
		}

		// remove base nodes without further field access
		boolean changed = true;
		while (changed) {
			List<PDGNode> toRemove = new LinkedList<PDGNode>();
			for (PDGNode node : ddg.vertexSet()) {
				if (!node.isFieldPtr() && ddg.outDegreeOf(node) == 0) {
					toRemove.add(node);
				}
			}

			changed = !toRemove.isEmpty();

			for (PDGNode node : toRemove) {
				ddg.removeNode(node);
			}
		}

		return ddg;
	}

}
