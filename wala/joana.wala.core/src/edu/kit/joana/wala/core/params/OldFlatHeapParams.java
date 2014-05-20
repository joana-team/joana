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
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.traverse.NumberedDFSDiscoverTimeIterator;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGField;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.SDGBuilder;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 * @deprecated
 */
public class OldFlatHeapParams {

	private final SDGBuilder sdg;

	public static void compute(SDGBuilder sdg, IProgressMonitor progress) throws CancelException {
		OldFlatHeapParams hp = new OldFlatHeapParams(sdg);
		hp.run(progress);
	}

	private OldFlatHeapParams(SDGBuilder sdg) {
		this.sdg = sdg;
	}

	private boolean createOutputParamFor(final TypeReference type) {
		return !type.isPrimitiveType() && !sdg.isImmutableNoOutParam(type);
	}

	private void run(IProgressMonitor progress) throws CancelException {
		final Map<PDGNode, PDGNode[]> entry2out = new HashMap<PDGNode, PDGNode[]>();

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
		}

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
						assert actOuts.length == formOuts.length;

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
		}

		// collect reachable points-to elements for reachable fields for each parameter
		for (final PDG pdg : sdg.getAllPDGs()) {
			final Map<PDGNode, OrdinalSet<InstanceKey>> node2ptsMod = new HashMap<PDGNode, OrdinalSet<InstanceKey>>();
			final Map<PDGNode, OrdinalSet<InstanceKey>> node2ptsRef = new HashMap<PDGNode, OrdinalSet<InstanceKey>>();

			final IR ir = pdg.cgNode.getIR();

			if (ir == null) {
				continue;
			}

			for (int i = 0; i < pdg.params.length; i++) {
				if (!pdg.getParamType(i).isPrimitiveType()) {
					final PDGNode formIn = pdg.params[i];
					final int ssaVar = ir.getParameter(i);

					if (ssaVar >= 0) {
						final OrdinalSet<InstanceKey> ptsSet = findReachableInstances(pdg.cgNode, ssaVar);
						node2ptsMod.put(formIn, ptsSet);
					}
				}
			}

			final PDGNode[] formOuts = entry2out.get(pdg.entry);
			for (int i = 0; i < formOuts.length; i++) {
				final PDGNode formOut = formOuts[i];

				if (formOut != null) {
					final PDGNode formIn = pdg.params[i];
					final OrdinalSet<InstanceKey> ptsSet = node2ptsMod.get(formIn);
					node2ptsRef.put(formOut, ptsSet);
				}
			}

			final TypeReference retType = pdg.getMethod().getReturnType();
			if (retType != TypeReference.Void && !retType.isPrimitiveType()) {
				for (final PDGNode retNode : pdg.getReturns()) {
					final SSAReturnInstruction ret = (SSAReturnInstruction) pdg.getInstruction(retNode);
					final int ssaVar = ret.getResult();
					final OrdinalSet<InstanceKey> ptsSet = findReachableInstances(pdg.cgNode, ssaVar);

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
							final OrdinalSet<InstanceKey> ptsSet = findReachableInstances(pdg.cgNode, ssaVar);
							node2ptsRef.put(actIn, ptsSet);
						}
					}
				}

				final PDGNode actOuts[] = entry2out.get(call);
				for (int i = 0; i < actIns.length; i++) {
					final PDGNode actOut = actOuts[i];

					if (actOut != null) {
						final PDGNode actIn = actIns[i];
						final OrdinalSet<InstanceKey> ptsSet = node2ptsRef.get(actIn);
						node2ptsMod.put(actOut, ptsSet);
					}
				}

				final TypeReference callRetType = invk.getDeclaredTarget().getReturnType();
				if (callRetType != TypeReference.Void && !callRetType.isPrimitiveType()) {
					final int ssaVar =  invk.getReturnValue(0);
					final OrdinalSet<InstanceKey> ptsSet = findReachableInstances(pdg.cgNode, ssaVar);
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
							final OrdinalSet<InstanceKey> ptsSet = pointsToArrayField(pdg.cgNode, ssaVarBase);
							node2ptsRef.put(read.accfield, ptsSet);
						}
					} else {
						final SSAGetInstruction get = (SSAGetInstruction) pdg.getInstruction(read.node);
						final int ssaVarBase = get.getRef();

						if (ssaVarBase >= 0) {
							final IField field = sdg.getClassHierarchy().resolveField(get.getDeclaredField());
							final OrdinalSet<InstanceKey> ptsSet = pointsToObjectField(pdg.cgNode, ssaVarBase, field);
							node2ptsRef.put(read.accfield, ptsSet);
						}
					}

//					final SSAInstruction get = pdg.getInstruction(read.node);
//					final int ssaVar = get.getDef();
//					if (ssaVar >= 0) {
//						final OrdinalSet<InstanceKey> ptsSet = findReachableInstances(pdg.cgNode, ssaVar);
//						node2ptsRef.put(read.accfield, ptsSet);
//					}
				}
			}

			for (final PDGField write : pdg.getFieldWrites()) {
				if (!write.field.isStatic()) {
					if (write.field.isArray()) {
						final SSAArrayStoreInstruction asi = (SSAArrayStoreInstruction) pdg.getInstruction(write.node);
						final int ssaVarBase = asi.getArrayRef();

						if (ssaVarBase >= 0) {
							final OrdinalSet<InstanceKey> ptsSet = pointsToArrayField(pdg.cgNode, ssaVarBase);
							node2ptsMod.put(write.accfield, ptsSet);
						}
					} else {
						final SSAPutInstruction put = (SSAPutInstruction) pdg.getInstruction(write.node);
						final int ssaVarBase = put.getRef();

						if (ssaVarBase >= 0) {
							final IField field = sdg.getClassHierarchy().resolveField(put.getDeclaredField());
							final OrdinalSet<InstanceKey> ptsSet = pointsToObjectField(pdg.cgNode, ssaVarBase, field);
							node2ptsMod.put(write.accfield, ptsSet);
						}
					}
				}
			}

			// add data flow for each pdg
			FlatHeapParamsDataFlow.compute(pdg, node2ptsMod, node2ptsRef,
					(sdg.cfg.accessPath ? PDGEdge.Kind.DATA_ALIAS : PDGEdge.Kind.DATA_HEAP), progress);
		}
	}

	private OrdinalSet<InstanceKey> findReachableInstances(final CGNode node, final int ssaVar) {
		final PointerAnalysis<InstanceKey> pts = sdg.getPointerAnalysis();
		final PointerKey pkStart = pts.getHeapModel().getPointerKeyForLocal(node, ssaVar);

		return findReachableInstances(pts, pkStart);
	}

	private OrdinalSet<InstanceKey> pointsToArrayField(final CGNode node, final int ssaVarBase) {
		final PointerAnalysis<InstanceKey> pts = sdg.getPointerAnalysis();

		final PointerKey pkBase = pts.getHeapModel().getPointerKeyForLocal(node, ssaVarBase);
		final OrdinalSet<InstanceKey> basePts = pts.getPointsToSet(pkBase);

		return basePts;

		// no field sensitivity atm

//		OrdinalSet<InstanceKey> ptsSet = OrdinalSet.empty();
//		for (final InstanceKey ik : basePts) {
//			final PointerKey fieldPk = pts.getHeapModel().getPointerKeyForArrayContents(ik);
//			final OrdinalSet<InstanceKey> ptsPart = pts.getPointsToSet(fieldPk);
//			ptsSet = OrdinalSet.unify(ptsSet, ptsPart);
//		}
//
//		return ptsSet;
	}

	private OrdinalSet<InstanceKey> pointsToObjectField(final CGNode node, final int ssaVarBase, final IField field) {
		final PointerAnalysis<InstanceKey> pts = sdg.getPointerAnalysis();

		final PointerKey pkBase = pts.getHeapModel().getPointerKeyForLocal(node, ssaVarBase);
		final OrdinalSet<InstanceKey> basePts = pts.getPointsToSet(pkBase);

		return basePts;

		// no field sensitivity atm

//		OrdinalSet<InstanceKey> ptsSet = OrdinalSet.empty();
//		for (final InstanceKey ik : basePts) {
//			final PointerKey fieldPk = pts.getHeapModel().getPointerKeyForInstanceField(ik, field);
//			final OrdinalSet<InstanceKey> ptsPart = pts.getPointsToSet(fieldPk);
//			ptsSet = OrdinalSet.unify(ptsSet, ptsPart);
//		}
//
//		return ptsSet;
	}

	private static OrdinalSet<InstanceKey> findReachableInstances(final PointerAnalysis<InstanceKey> pts,
			final PointerKey pkStart) {
		final HeapGraph hg = pts.getHeapGraph();
		if (!hg.containsNode(pkStart)) {
			return OrdinalSet.empty();
		}

		final OrdinalSetMapping<InstanceKey> mapping = pts.getInstanceKeyMapping();

		final BitVectorIntSet resultSet = new BitVectorIntSet();

		final NumberedDFSDiscoverTimeIterator<Object> dfsDiscover =
				new NumberedDFSDiscoverTimeIterator<Object>(hg, pkStart);
		while (dfsDiscover.hasNext()) {
			Object obj = dfsDiscover.next();
			if (obj instanceof InstanceKey) {
				InstanceKey ik = (InstanceKey) obj;
				final int ikNum = mapping.getMappedIndex(ik);
				resultSet.add(ikNum);
			}
		}

		final OrdinalSet<InstanceKey> reachable = new OrdinalSet<InstanceKey>(resultSet, mapping);

		return reachable;
	}

//	private static OrdinalSet<InstanceKey> findReachableInstances(final PointerAnalysis pts, final List<PointerKey> pkStart) {
//		final HeapGraph hg = pts.getHeapGraph();
//		final List<PointerKey> toUse = new LinkedList<PointerKey>();
//		for (final PointerKey pk : pkStart) {
//			if (hg.containsNode(pk)) {
//				toUse.add(pk);
//			}
//		}
//
//		if (toUse.isEmpty()) {
//			return OrdinalSet.empty();
//		}
//
//		final BitVectorIntSet resultSet = new BitVectorIntSet();
//
//		final NumberedDFSDiscoverTimeIterator<Object> dfsDiscover = new NumberedDFSDiscoverTimeIterator<Object>(hg, toUse.iterator());
//		while (dfsDiscover.hasNext()) {
//			Object obj = dfsDiscover.next();
//			if (obj instanceof InstanceKey) {
//				InstanceKey ik = (InstanceKey) obj;
//				final int ikNum = hg.getNumber(ik);
//				resultSet.add(ikNum);
//			}
//		}
//
//		final OrdinalSet<InstanceKey> reachable = new OrdinalSet<InstanceKey>(resultSet, pts.getInstanceKeyMapping());
//
//		return reachable;
//	}

}
