/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params;

import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.AbstractLocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphReachability;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGField;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.SDGBuilder;

public class FlatHeapParams {

	private final SDGBuilder sdg;
	private final Map<CGNode, OrdinalSet<PointerKey>> cg2mod;
	private final Map<CGNode, OrdinalSet<PointerKey>> cg2ref;
	private final PointerAnalysis<InstanceKey> pts;
	private final OrdinalSetMapping<PointerKey> mapping;

	public static final boolean DO_REACHABILITY_PER_METHOD = true;

	public static void compute(SDGBuilder sdg, IProgressMonitor progress) throws CancelException {
		FlatHeapParams hp = new FlatHeapParams(sdg);
		hp.run(progress);
	}

	private static boolean isRelevantPointerKey(final Object obj) {
		return obj instanceof InstanceKey || obj instanceof StaticFieldKey || obj instanceof InstanceFieldPointerKey;
	}

	private FlatHeapParams(SDGBuilder sdg) {
		this.sdg = sdg;
		this.pts = sdg.getPointerAnalysis();
		this.mapping = MutableMapping.make();
		for (PointerKey pk : pts.getPointerKeys()) {
			if (isRelevantPointerKey(pk)) {
				mapping.add(pk);
			}
		}
		this.cg2mod = new HashMap<CGNode, OrdinalSet<PointerKey>>();
		this.cg2ref = new HashMap<CGNode, OrdinalSet<PointerKey>>();
	}

	private GraphReachability<Object, Object> computeReachblePointerKeys(final OrdinalSet<PointerKey> mod,
			final OrdinalSet<PointerKey> ref, final IProgressMonitor progress) throws CancelException {
		HeapGraph<InstanceKey> heap = pts.getHeapGraph();

		Graph<Object> pruned = GraphSlicer.prune(heap, new Predicate<Object>() {

			@Override
			public boolean test(Object t) {
				return t instanceof InstanceKey || t instanceof AbstractLocalPointerKey
						|| mod.contains((PointerKey) t) || ref.contains((PointerKey) t);
			}
		});

		GraphReachability<Object, Object> reach = new GraphReachability<Object, Object>(pruned, new Predicate<Object>() {
			@Override
			public boolean test(final Object o) {
				return isRelevantPointerKey(o);
			}

		});

		reach.solve(progress);

		return reach;
	}

	private boolean createOutputParamFor(final TypeReference type) {
		return !type.isPrimitiveType() && !sdg.isImmutableNoOutParam(type);
	}

	private void run(IProgressMonitor progress) throws CancelException {
		buildModRefMaps(progress);

		final Map<PDGNode, PDGNode[]> entry2out = new HashMap<PDGNode, PDGNode[]>();

        int progressCtr = 0;
        progress.beginTask("interproc: adding data flow for heap fields...", 3 * sdg.getAllPDGs().size());
        progress.subTask("add out nodes for each non-primitive parameter (flat FieldPropagation)");

		// add out nodes for each non-primitive parameter
		for (final PDG pdg : sdg.getAllPDGs()) {
			// primitive types nodes are null
			final PDGNode[] formOuts = new PDGNode[pdg.params.length];

			for (int i = 0; i < pdg.params.length; i++) {
				final PDGNode formIn = pdg.params[i];
				final TypeReference type = pdg.getParamType(i);

				if (createOutputParamFor(type)) {
					final PDGNode formOut = pdg.addOutputFieldChildTo(pdg.entry, formIn.getLabel(),
							formIn.getBytecodeName(), formIn.getBytecodeIndex(), formIn.getTypeRef());
					formOuts[i] = formOut;
				}
			}

			entry2out.put(pdg.entry, formOuts);

			for (final PDGNode call : pdg.getCalls()) {
				final PDGNode[] actIns = pdg.getParamIn(call);
				final PDGNode[] actOuts = new PDGNode[actIns.length];

				for (int i = 0; i < actIns.length; i++) {
					final PDGNode actIn = actIns[i];
					final TypeReference type = pdg.getParamType(call, i);

					if (createOutputParamFor(type)) {
						final PDGNode actOut = pdg.addOutputFieldChildTo(call, actIn.getLabel(),
								actIn.getBytecodeName(), actIn.getBytecodeIndex(), actIn.getTypeRef());
						actOuts[i] = actOut;
					}
				}

				entry2out.put(call, actOuts);
			}

            progress.worked(progressCtr++);
		}

        progress.subTask("connect form-outs of called procedures with act-outs (flat FieldPropagation)");
		// connect form-outs of called procedures with act-outs
		for (final PDG pdg : sdg.getAllPDGs()) {
			for (final PDGNode call : pdg.getCalls()) {
				final PDGNode[] actOuts = entry2out.get(call);
				assert actOuts != null;

				for (final PDGEdge out : pdg.outgoingEdgesOf(call)) {
					if (out.kind == PDGEdge.Kind.CALL_STATIC || out.kind == PDGEdge.Kind.CALL_VIRTUAL) {
						final PDGNode calleeEntry = out.to;
						final PDGNode[] formOuts = entry2out.get(calleeEntry);
						final PDG callee = sdg.getPDGforId(calleeEntry.getPdgId());
						assert formOuts != null;
						if (actOuts.length != formOuts.length) {
                            System.err.println( "Error actOuts:" + actOuts.length + " != " +
                                                "formOuts:" + formOuts.length + " in count! In PDG:" + pdg.toString() );
                            System.err.println( "Act Outs are:" );
                            for (final PDGNode o : actOuts) {
                                System.err.println("  " + o.toString());
                            }
                            System.err.println( "\nForm Outs are:" );
                            for (final PDGNode o : formOuts) {
                                System.err.println("  " + ((o == null)?"null": o.toString()));
                            }
                            assert (actOuts.length == formOuts.length);
                        }

						for (int i = 0; i < actOuts.length; i++) {
							final PDGNode actOut = actOuts[i];
							if (actOut != null) {
								// primitive type (and immutable type) params have no act out
								callee.addVertex(actOut);
								final PDGNode formOut = formOuts[i];

								if (formOut != null) {
									callee.addEdge(formOut, actOut, PDGEdge.Kind.PARAMETER_OUT);
								}
							}
						}
					}
				}
			}
            progress.worked(progressCtr++);
		}

		GraphReachability<Object, Object> heapReach;
		if (!DO_REACHABILITY_PER_METHOD) {
			// solve reachability of heapgraph for whole program
			heapReach = new GraphReachability<Object, Object>(pts.getHeapGraph(), new Predicate<Object>() {
				@Override
				public boolean test(final Object o) {
					return isRelevantPointerKey(o) || o instanceof PointerKey;
				}

			});

			heapReach.solve(progress);
		}

        progress.subTask("collect reachable points-to elements for reachable fields for each parameter (flat FieldPropagation)");
		// collect reachable points-to elements for reachable fields for each parameter
		for (final PDG pdg : sdg.getAllPDGs()) {
			final Map<PDGNode, OrdinalSet<PointerKey>> node2ptsMod = new HashMap<PDGNode, OrdinalSet<PointerKey>>();
			final Map<PDGNode, OrdinalSet<PointerKey>> node2ptsRef = new HashMap<PDGNode, OrdinalSet<PointerKey>>();

			final IR ir = pdg.cgNode.getIR();

			if (ir == null) {
				continue;
			}

			if (DO_REACHABILITY_PER_METHOD) {
				// solve reachability of heapgraph for single method
				final OrdinalSet<PointerKey> mod = cg2mod.get(pdg.cgNode);
				final OrdinalSet<PointerKey> ref = cg2ref.get(pdg.cgNode);
				heapReach = computeReachblePointerKeys(mod, ref, progress);
			}

			for (int i = 0; i < pdg.params.length; i++) {
				if (!pdg.getParamType(i).isPrimitiveType()) {
					final PDGNode formIn = pdg.params[i];
					final int ssaVar = ir.getParameter(i);

					if (ssaVar >= 0) {
						final OrdinalSet<PointerKey> ptsSet = findReachableInstances(heapReach, pdg.cgNode, ssaVar);
						node2ptsMod.put(formIn, ptsSet);
					}
				}
			}

			final PDGNode[] formOuts = entry2out.get(pdg.entry);
			for (int i = 0; i < formOuts.length; i++) {
				final PDGNode formOut = formOuts[i];

				if (formOut != null) {
					final PDGNode formIn = pdg.params[i];
					final OrdinalSet<PointerKey> ptsSet = node2ptsMod.get(formIn);
					node2ptsRef.put(formOut, ptsSet);
					pdg.addEdge(formIn, formOut, PDGEdge.Kind.DATA_DEP);
				}
			}

			final TypeReference retType = pdg.getMethod().getReturnType();
			if (retType != TypeReference.Void && !retType.isPrimitiveType()) {
				for (final PDGNode retNode : pdg.getReturns()) {
					final SSAReturnInstruction ret = (SSAReturnInstruction) pdg.getInstruction(retNode);
					final int ssaVar = ret.getResult();
					final OrdinalSet<PointerKey> ptsSet = findReachableInstances(heapReach, pdg.cgNode, ssaVar);

					node2ptsRef.put(retNode, ptsSet);
				}
			}

			for (final PDGNode call : pdg.getCalls()) {
				final PDGNode[] actIns = pdg.getParamIn(call);

				final SSAAbstractInvokeInstruction invk = (SSAAbstractInvokeInstruction) pdg.getInstruction(call);

				for (int i = 0; i < actIns.length; i++) {

					if (!pdg.getParamType(call, i).isPrimitiveType()) {
						final PDGNode actIn = actIns[i];
						final int ssaVar = invk.getUse(i);

						if (ssaVar >= 0) {
							final OrdinalSet<PointerKey> ptsSet = findReachableInstances(heapReach, pdg.cgNode, ssaVar);
//							if (actIn.getId() == 24) {
//								for (PointerKey pk : ptsSet) {
//									System.out.println(pk);
//								}
//							}
							node2ptsRef.put(actIn, ptsSet);
						}
					}
				}

				final PDGNode actOuts[] = entry2out.get(call);
				for (int i = 0; i < actIns.length; i++) {
					final PDGNode actOut = actOuts[i];

					if (actOut != null) {
						final PDGNode actIn = actIns[i];
						final OrdinalSet<PointerKey> ptsSet = node2ptsRef.get(actIn);
						node2ptsMod.put(actOut, ptsSet);
					}
				}

				final TypeReference callRetType = invk.getDeclaredTarget().getReturnType();
				if (callRetType != TypeReference.Void && !callRetType.isPrimitiveType()) {
					final int ssaVar =  invk.getReturnValue(0);
					final OrdinalSet<PointerKey> ptsSet = findReachableInstances(heapReach, pdg.cgNode, ssaVar);
					final PDGNode retNode = pdg.getReturnOut(call);
					node2ptsMod.put(retNode, ptsSet);
				}
			}

			// create mod/ref sets for get & set instructions
			for (final PDGField read : pdg.getFieldReads()) {
				if (!read.field.isStatic()) {
					if (read.field.isArray()) {
						final SSAArrayLoadInstruction ali = (SSAArrayLoadInstruction) pdg.getInstruction(read.node);
						final int ssaVarBase = ali.getArrayRef();

						if (ssaVarBase >= 0) {
							final OrdinalSet<PointerKey> ptsSet = pointsToArrayField(pdg.cgNode, ssaVarBase);
							node2ptsRef.put(read.accfield, ptsSet);
						}
					} else {
						final SSAGetInstruction get = (SSAGetInstruction) pdg.getInstruction(read.node);
						final int ssaVarBase = get.getRef();

						if (ssaVarBase >= 0) {
							final IField field = sdg.getClassHierarchy().resolveField(get.getDeclaredField());
							final OrdinalSet<PointerKey> ptsSet = pointsToObjectField(pdg.cgNode, ssaVarBase, field);
							node2ptsRef.put(read.accfield, ptsSet);
						}
					}
				}
			}

			for (final PDGField write : pdg.getFieldWrites()) {
				if (!write.field.isStatic()) {
					if (write.field.isArray()) {
						final SSAArrayStoreInstruction asi = (SSAArrayStoreInstruction) pdg.getInstruction(write.node);
						final int ssaVarBase = asi.getArrayRef();

						if (ssaVarBase >= 0) {
							final OrdinalSet<PointerKey> ptsSet = pointsToArrayField(pdg.cgNode, ssaVarBase);
							node2ptsMod.put(write.accfield, ptsSet);
						}
					} else {
						final SSAPutInstruction put = (SSAPutInstruction) pdg.getInstruction(write.node);
						final int ssaVarBase = put.getRef();

						if (ssaVarBase >= 0) {
							final IField field = sdg.getClassHierarchy().resolveField(put.getDeclaredField());
							final OrdinalSet<PointerKey> ptsSet = pointsToObjectField(pdg.cgNode, ssaVarBase, field);
							node2ptsMod.put(write.accfield, ptsSet);
						}
					}
				}
			}

			// add data flow for each pdg - access path computation tries to compute heap dependencies that are not due
			// to aliasing. It starts with assuming all heap deps are aliases.
			FlatHeapParamsDataFlow.compute(pdg, node2ptsMod, node2ptsRef,
					(sdg.cfg.accessPath ? PDGEdge.Kind.DATA_ALIAS : PDGEdge.Kind.DATA_HEAP), progress);
            progress.worked(progressCtr++);
		}
	}

	private void buildModRefMaps(IProgressMonitor progress) throws CancelException {
		final ModRef<InstanceKey> mref = ModRef.make();
		// used non-pruned callgraph at this level
		final Map<CGNode, OrdinalSet<PointerKey>> mod =  mref.computeMod(sdg.getNonPrunedWalaCallGraph(), pts);
		final Map<CGNode, OrdinalSet<PointerKey>> ref =  mref.computeRef(sdg.getNonPrunedWalaCallGraph(), pts);

		for (final CGNode cn : mod.keySet()) {
			MonitorUtil.throwExceptionIfCanceled(progress);

			final OrdinalSet<PointerKey> modset = mod.get(cn);

			if (modset == null) {
				OrdinalSet<PointerKey> empty = OrdinalSet.empty();
				cg2mod.put(cn, empty);
			} else {
				final BitVectorIntSet set = new BitVectorIntSet();

				for (PointerKey pk : modset) {
					if (isRelevantPointerKey(pk)) {
						if (mapping.hasMappedIndex(pk)) {
							set.add(mapping.getMappedIndex(pk));
						} else {
							set.add(mapping.add(pk));
						}
					}
				}

				OrdinalSet<PointerKey> ms = new OrdinalSet<PointerKey>(set, mapping);
				cg2mod.put(cn, ms);
			}

		}

		for (final CGNode cn : ref.keySet()) {
			MonitorUtil.throwExceptionIfCanceled(progress);

			final OrdinalSet<PointerKey> refset = ref.get(cn);

			if (refset == null) {
				OrdinalSet<PointerKey> empty = OrdinalSet.empty();
				cg2ref.put(cn, empty);
			} else {
				final BitVectorIntSet set = new BitVectorIntSet();

				for (PointerKey pk : refset) {
					if (isRelevantPointerKey(pk)) {
						if (mapping.hasMappedIndex(pk)) {
							set.add(mapping.getMappedIndex(pk));
						} else {
							set.add(mapping.add(pk));
						}
					}
				}

				OrdinalSet<PointerKey> rs = new OrdinalSet<PointerKey>(set, mapping);
				cg2ref.put(cn, rs);
			}

		}

	}

	private OrdinalSet<PointerKey> findReachableInstances(final GraphReachability<Object, Object> heapReach,
			final CGNode node, final int ssaVar) throws CancelException {
		final PointerKey pkStart = pts.getHeapModel().getPointerKeyForLocal(node, ssaVar);

		return findReachableInstances(heapReach, node, pkStart);
	}

	private OrdinalSet<PointerKey> pointsToArrayField(final CGNode node, final int ssaVarBase) {
		final HeapModel m = pts.getHeapModel();
		final PointerKey pkBase = m.getPointerKeyForLocal(node, ssaVarBase);
		final OrdinalSet<InstanceKey> basePts = pts.getPointsToSet(pkBase);

		final BitVectorIntSet set = new BitVectorIntSet();

		for (InstanceKey ik : basePts) {
			final PointerKey pk = m.getPointerKeyForArrayContents(ik);
			final int index = mapping.getMappedIndex(pk);
			set.add(index);
		}

		return new OrdinalSet<PointerKey>(set, mapping);
	}

	private OrdinalSet<PointerKey> pointsToObjectField(final CGNode node, final int ssaVarBase, final IField field) {
		final HeapModel m = pts.getHeapModel();
		final PointerKey pkBase = m.getPointerKeyForLocal(node, ssaVarBase);
		final OrdinalSet<InstanceKey> basePts = pts.getPointsToSet(pkBase);

		final BitVectorIntSet set = new BitVectorIntSet();

		for (InstanceKey ik : basePts) {
			final PointerKey pk = m.getPointerKeyForInstanceField(ik, field);
			final int index = mapping.getMappedIndex(pk);
			set.add(index);
		}

		return new OrdinalSet<PointerKey>(set, mapping);
	}

	private OrdinalSet<PointerKey> findReachableInstances(final GraphReachability<Object, Object> heapReach,
			final CGNode node, final PointerKey pkStart) throws CancelException {
		final HeapGraph<InstanceKey> hg = pts.getHeapGraph();
		if (!hg.containsNode(pkStart)) {
			return OrdinalSet.empty();
		}

		final OrdinalSet<Object> objReach = heapReach.getReachableSet(pkStart);
		final BitVectorIntSet resultSet = new BitVectorIntSet();

		final OrdinalSet<PointerKey> mod = cg2mod.get(node);
		final OrdinalSet<PointerKey> ref = cg2ref.get(node);

		if (mod.isEmpty() && ref.isEmpty()) {
			return OrdinalSet.empty();
		}

		for (Object obj : objReach) {
			if (obj instanceof PointerKey) {
				PointerKey pk = (PointerKey) obj;

				if (mod.contains(pk) || ref.contains(pk)) {
					final int ikNum = mapping.getMappedIndex(pk);
					resultSet.add(ikNum);
				}
			}
		}

		// work around for object fields of primitive type.
		// wala does not include pointerkeys to object fields of a primitive type in the heapgraph
		// in order to check if they are reachable non the less, we have to check if the object instance
		// they belong to is reachable
		for (PointerKey pk : mod) {
			if (pk instanceof InstanceFieldPointerKey) {
				final int id = mapping.getMappedIndex(pk);
				if (!resultSet.contains(id)) {
					final InstanceFieldPointerKey ifpk = (InstanceFieldPointerKey) pk;
					final InstanceKey ik = ifpk.getInstanceKey();
					if (objReach.contains(ik)) {
						resultSet.add(id);
					}
				}
			}
		}

		for (PointerKey pk : ref) {
			if (pk instanceof InstanceFieldPointerKey) {
				final int id = mapping.getMappedIndex(pk);
				if (!resultSet.contains(id)) {
					final InstanceFieldPointerKey ifpk = (InstanceFieldPointerKey) pk;
					final InstanceKey ik = ifpk.getInstanceKey();
					if (objReach.contains(ik)) {
						resultSet.add(id);
					}
				}
			}
		}

		final OrdinalSet<PointerKey> reachable = new OrdinalSet<PointerKey>(resultSet, mapping);

		return reachable;
	}

}
