/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.interference;

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.escape.TrivialMethodEscape;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.SparseIntSet;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterFieldFactory;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.IPointerAnalysis;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.PointsToWrapper;
import edu.kit.joana.deprecated.jsdg.util.Debug;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.IKey2Origin;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.IKey2Origin.InstanceKeyOrigin;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * This interference computation is used for the original wala system dependence
 * graph.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class WalaSDGInterferenceComputation {

	private final SDG<InstanceKey> sdg;
	private final CallGraph cg;
	private final HeapModel heap;

	private final boolean optimizeThisAccess;
	private final TrivialMethodEscape escape;
	private final IKey2Origin k2o;
	private IMethod threadStart = null;
	private Set<CGNode> threadStarts = null;
	private final IPointerAnalysis pts;
	private static final boolean DO_ARRAYS = true;
	private static final boolean DO_SYNC = true;
	private static final boolean DO_SYNC_METHODS = DO_SYNC && true;
	private static final int THIS_PARAM_NUM = 0; // valuenumber(variable name) of this pointer
	private static final String CLASS_FIELD_NAME_PREFIX = "class$";

	private final TIntObjectHashMap<MutableIntSet> pdgId2threadIds;


	/*
	 * A map containing all possible thread instances for the interference
	 * analysis. Each thread has an id that is
	 * mapped to a set of pdg-ids which may be called during the execution of
	 * this thread.
	 */
	private final TIntObjectHashMap<IntSet> threads;

	/*
	 * Maps the pdg-id of a thread started run() method to a set of thread-ids
	 * (typically two - to model the fact that this run method may run in parallel to itself)
	 * This is done to retrieve a shiny new enumeration starting at 0 (for the main method)
	 */
	private final TIntObjectHashMap<IntSet> threadIds;

	private final TIntObjectHashMap<MutableIntSet> readWrite;
	private final TIntObjectHashMap<MutableIntSet> writeWrite;

	public WalaSDGInterferenceComputation(SDG<InstanceKey> sdg, CallGraph cg, boolean optimizeThisAccess, boolean useEscapeAnalysis,
			HeapGraph<InstanceKey> hg, IKey2Origin k2o) {
		this.sdg = sdg;
		this.cg = cg;
		this.k2o = k2o;
		this.threadIds = new TIntObjectHashMap<IntSet>();
		this.threads = new TIntObjectHashMap<IntSet>();
		this.optimizeThisAccess = optimizeThisAccess;
		this.heap = hg.getHeapModel();
		this.pdgId2threadIds = new TIntObjectHashMap<MutableIntSet>();
		this.readWrite = new TIntObjectHashMap<MutableIntSet>();
		this.writeWrite = new TIntObjectHashMap<MutableIntSet>();

		//TODO here may be inserted a demand driven pointer analysis
		//this.pts = new PointsToWrapper(SDGFactory.pts, hg.getPointerAnalysis());
		this.pts = new PointsToWrapper(null, hg.getPointerAnalysis());

		if (useEscapeAnalysis) {
			escape = new TrivialMethodEscape(cg, hg);
		} else {
			escape = null;
		}
	}

	public final void compute(IProgressMonitor progress) throws CancelException, WalaException {
		computeThreadIds(progress);

		if (threadStarts != null) {
			computeInterference(progress);
		}
	}

	public final TIntObjectHashMap<MutableIntSet> getReadWrites() {
		return readWrite;
	}

	public final TIntObjectHashMap<MutableIntSet> getWriteWrites() {
		return writeWrite;
	}

	/**
	 * Look for each heap accessing node which other heap accessing node may
	 * happen in parallel and may be referring to the same location.
	 * @throws CancelException
	 * @throws WalaException
	 */
	private void computeInterference(IProgressMonitor progress) throws CancelException, WalaException {
		Log.info("Computing read-write/write-write interference for threads");

		for (CGNode cgNode : cg) {
			PDG<InstanceKey> pdg = sdg.getPDG(cgNode);

			Set<HeapWrite> writes = getHeapWrites(pdg);

			if (writes.isEmpty()) {
				// a pdg without heap access does not interfere with anything
				// the interferences thay may happen because of read statements
				// only appear when there is a aliasing wirte statement in another
				// pdg. We skip them here because they will be added later on
				// while handling the pdg with the intefereing write.
				continue;
			}

			for (CGNode cgNodeCur : cg) {
				PDG pdgCur = sdg.getPDG(cgNodeCur);

				if (mayRunInParallelThreads(pdg, pdgCur)) {
					Set<HeapRead> readsCur = getHeapReads(pdgCur);
					Set<HeapWrite> writesCur = getHeapWrites(pdgCur);

					if (readsCur.isEmpty() && writesCur.isEmpty()) {
						// a pdg without heap access does not interfere with anything
						continue;
					}

					computeInterference(writes, readsCur, writesCur);
				}

				if (progress.isCanceled()) {
					throw CancelException.make("Computing interference canceled.");
				}
			}

			if (progress.isCanceled()) {
				throw CancelException.make("Computing interference canceled.");
			}

			progress.worked(1);
		}
	}

	private void computeInterference(Set<HeapWrite> writes1,
			Set<HeapRead> reads2, Set<HeapWrite> writes2) {
		for (HeapWrite write : writes1) {
			for (HeapRead read2 : reads2) {
				if (write.isAliasing(read2)) {
					addReadWriteInterference(write, read2);
				}
			}
			for (HeapWrite write2 : writes2) {
				if (write.isAliasing(write2)) {
					addWriteWriteInterference(write, write2);
				}
			}
		}
	}

	private void addReadWriteInterference(Statement from, Statement to) {
		int fromId = sdg.getNumber(from);
		int toId = sdg.getNumber(to);

		addToMapSet(readWrite, fromId, toId);
	}

	private void addWriteWriteInterference(Statement from, Statement to) {
		int fromId = sdg.getNumber(from);
		int toId = sdg.getNumber(to);

		addToMapSet(writeWrite, fromId, toId);
	}

//	private Set<HeapAccess> printed = HashSetFactory.make();

	private void addReadWriteInterference(HeapWrite write, HeapRead read) {
		Statement ewrite = write.getExpr();
		Statement eread = read.getExpr();

		addReadWriteInterference(ewrite, eread);

		if (Debug.Var.PRINT_THREAD_INTERFERENCES.isSet()) {
			CGNode pdgWrite = cg.getNode(write.getPdgId());
			CGNode pdgRead = cg.getNode(read.getPdgId());

			Log.info("Read-write: " + Util.methodName(pdgWrite.getMethod())
					+ "{" + ewrite + "} - "
					+ Util.methodName(pdgRead.getMethod()) + "{" + eread + "}");
		}

//		if (Debug.Var.DUMP_HEAP_GRAPH.isSet()) {
//			if (!printed.contains(write)) {
//				String name = write.toString();
//				PrettyWalaNames.dumpHeapGraph(name, hg, write.getPk(), null);
//
//				printed.add(write);
//			}
//
//			if (!printed.contains(read)) {
//				String name = read.toString();
//				PrettyWalaNames.dumpHeapGraph(name, hg, read.getPk(), null);
//
//				printed.add(read);
//			}
//		}
	}

	private void addWriteWriteInterference(HeapWrite write1, HeapWrite write2) {
		Statement ewrite1 = write1.getExpr();
		Statement ewrite2 = write2.getExpr();

		addWriteWriteInterference(ewrite1, ewrite2);

		if (Debug.Var.PRINT_THREAD_INTERFERENCES.isSet()) {
			CGNode pdgWrite1 = cg.getNode(write1.getPdgId());
			CGNode pdgWrite2 = cg.getNode(write2.getPdgId());
			Log.info("Write-write: " + Util.methodName(pdgWrite1.getMethod())
					+ "{" + ewrite1 + "} - "
					+ Util.methodName(pdgWrite2.getMethod()) + "{" + ewrite2 + "}");
		}
	}

	private boolean mayRunInParallelThreads(PDG<InstanceKey> pdg1, PDG<InstanceKey> pdg2) {
		int pdg1Id = cg.getNumber(pdg1.getCallGraphNode());
		IntSet tids1 = getFromMapSet(pdgId2threadIds, pdg1Id);
		int pdg2Id = cg.getNumber(pdg2.getCallGraphNode());
		IntSet tids2 = getFromMapSet(pdgId2threadIds, pdg2Id);;
		// when thread ids are empty the pdg runs in no thread -> so it definitely
		// does not run in parallel
		// it also definitely does not run in parallel when both pdgs may only be
		// executed in a single thread and this thread is the same for both pdgs
		return !(tids1.isEmpty() || tids2.isEmpty() ||
			(tids1.size() == 1 && tids2.size() == 1 && tids1.sameValue(tids2)));
	}

	private boolean isThisPointerAccessInConstructor(IMethod method, SSAFieldAccessInstruction instr) {
		if (!optimizeThisAccess) {
			return false;
		}

		if (method.isInit()) {
			// number 1 is the magic value number of the this pointer
			return (!instr.isStatic() && instr.getRef() == 1);
		}

		return false;
	}

	private boolean isThisPointerAccessInConstructor(IMethod method, SSAArrayReferenceInstruction instr) {
		if (!optimizeThisAccess) {
			return false;
		}

		if (method.isInit()) {
			// number 1 is the magic value number of the this pointer
			return (instr.getArrayRef() == 1);
		}

		return false;
	}

	private Set<NormalStatement> findGets(PDG<InstanceKey> pdg) {
		Set<NormalStatement> gets = HashSetFactory.make();

		for (Statement stat : pdg) {
			if (stat.getKind() == Kind.NORMAL) {
				NormalStatement ns = (NormalStatement) stat;
				SSAInstruction ssa = ns.getInstruction();
				if (ssa instanceof SSAGetInstruction) {
					gets.add(ns);
				}
			}
		}

		return gets;
	}

	private Set<NormalStatement> findArrayGets(PDG<InstanceKey> pdg) {
		Set<NormalStatement> gets = HashSetFactory.make();

		for (Statement stat : pdg) {
			if (stat.getKind() == Kind.NORMAL) {
				NormalStatement ns = (NormalStatement) stat;
				SSAInstruction ssa = ns.getInstruction();
				if (ssa instanceof SSAArrayLoadInstruction) {
					gets.add(ns);
				}
			}
		}

		return gets;
	}

	private Set<NormalStatement> findSyncs(PDG<InstanceKey> pdg) {
		Set<NormalStatement> syncs = HashSetFactory.make();

		for (Statement stat : pdg) {
			if (stat.getKind() == Kind.NORMAL) {
				NormalStatement ns = (NormalStatement) stat;
				SSAInstruction ssa = ns.getInstruction();
				if (ssa instanceof SSAMonitorInstruction) {
					syncs.add(ns);
				}
			}
		}

		return syncs;
	}

	private static Statement findEntryNode(PDG<InstanceKey> pdg) {
		for (Statement st : pdg) {
			if (st.getKind() == Kind.METHOD_ENTRY) {
				return st;
			}
		}

		throw new IllegalStateException("A pdg must have an entry node! " + pdg);
	}

	private static Statement findExitNode(PDG<InstanceKey> pdg) {
		for (Statement st : pdg) {
			if (st.getKind() == Kind.METHOD_EXIT) {
				return st;
			}
		}

		throw new IllegalStateException("A pdg must have an exit node! " + pdg);
	}

	private Set<HeapRead> getHeapReads(PDG<InstanceKey> pdg) {
		Set<HeapRead> hreads = HashSetFactory.make();

		final CGNode cgNode = pdg.getCallGraphNode();
		final IClassHierarchy hierarchy = cg.getClassHierarchy();
		final int cgNodeId = cg.getNumber(cgNode);

		for (NormalStatement stat : findGets(pdg)) {
			SSAGetInstruction get = (SSAGetInstruction) stat.getInstruction();
			if (isThisPointerAccessInConstructor(cgNode.getMethod(), get)) {
				continue;
			}

			OrdinalSet<InstanceKey> base;
			if (get.isStatic()) {
				base = OrdinalSet.empty();
			} else {
				if (get.getRef() < 0) {
					// skip operations with illegal value numbers
					Log.warn("Skipping instruction because reference value number < 0: " + get);
					continue;
				}

				PointerKey pk = heap.getPointerKeyForLocal(cgNode, get.getRef());
				base = pts.getPointsToSet(pk);
			}

			IField ifield = hierarchy.resolveField(get.getDeclaredField());
			if (ifield == null) {
				// skip not resolvable fields;
				continue;
			}
			ParameterField field = ParameterFieldFactory.getFactory().getObjectField(ifield);
//			Set<PointerKey> pk = HashSetFactory.make(1);
//			if (get.isStatic()) {
//				pk.add(heap.getPointerKeyForStaticField(ifield));
//			} else {
//				pk.add(heap.getPointerKeyForLocal(cgNode, get.getDef()));
//			}

			HeapRead read = HeapAccess.createRead(stat, base, field, cgNodeId);
			hreads.add(read);
		}

		if (DO_ARRAYS) {
			for (NormalStatement stat : findArrayGets(pdg)) {
				SSAArrayLoadInstruction get = (SSAArrayLoadInstruction) stat.getInstruction();
				if (isThisPointerAccessInConstructor(cgNode.getMethod(), get)) {
					continue;
				}

				PointerKey pkBase = heap.getPointerKeyForLocal(cgNode, get.getArrayRef());
				OrdinalSet<InstanceKey> base = pts.getPointsToSet(pkBase);
				TypeReference tRef = get.getElementType();
				ParameterField field = ParameterFieldFactory.getFactory().getArrayField(tRef);

				HeapRead read = HeapAccess.createRead(stat, base, field, cgNodeId);
				hreads.add(read);
			}
		}

		// do sync statements
		if (DO_SYNC) {
			for (NormalStatement stat : findSyncs(pdg)) {
				SSAMonitorInstruction sync = (SSAMonitorInstruction) stat.getInstruction();

				final int valNum = sync.getRef();
				PointerKey pkBase = heap.getPointerKeyForLocal(cgNode, valNum);
				OrdinalSet<InstanceKey> base = pts.getPointsToSet(pkBase);
				ParameterField field = ParameterFieldFactory.getFactory().getLockField();

				HeapRead read = HeapAccess.createRead(stat, base, field, cgNodeId);
				hreads.add(read);
			}

			if (DO_SYNC_METHODS) {
				final IMethod im = cgNode.getMethod();
				if (im.isSynchronized()) {
					final Statement entry = findEntryNode(pdg);
					final Statement exit = findExitNode(pdg);

					if (!im.isStatic()) {
						int valNum = cgNode.getIR().getParameter(THIS_PARAM_NUM);
						PointerKey pks = pts.getHeapModel().getPointerKeyForLocal(cgNode, valNum);
						OrdinalSet<InstanceKey> base = pts.getPointsToSet(pks);
						ParameterField field = ParameterFieldFactory.getFactory().getLockField();

						HeapRead readEntry = HeapAccess.createRead(entry, base, field, cgNodeId);
						hreads.add(readEntry);
						HeapRead readExit = HeapAccess.createRead(exit, base, field, cgNodeId);
						hreads.add(readExit);
					} else {
						// search for static field class
						IClass cls = im.getDeclaringClass();
						IField classField = null;
						for (IField field : cls.getAllStaticFields()) {
							if (TypeReference.JavaLangClass.getName().equals(field.getFieldTypeReference().getName())) {
								if (field.getName().toString().startsWith(CLASS_FIELD_NAME_PREFIX)) {
									classField = field;
									break;
								}
							}
						}

						if (classField != null) {
							PointerKey pk = pts.getHeapModel().getPointerKeyForStaticField(classField);
							OrdinalSet<InstanceKey> base = pts.getPointsToSet(pk);
							ParameterField field = ParameterFieldFactory.getFactory().getLockField();

							HeapRead readEntry = HeapAccess.createRead(entry, base, field, cgNodeId);
							hreads.add(readEntry);
							HeapRead readExit = HeapAccess.createRead(exit, base, field, cgNodeId);
							hreads.add(readExit);
						} else {
							// no class field found - so we create an artificial one. this is some sort of HACK as
							// the WALA points-to analysis does not know about this field...
							OrdinalSet<InstanceKey> base = pts.getArtificialClassFieldPts(cls);
							ParameterField field = ParameterFieldFactory.getFactory().getClassLockField();

							HeapRead readEntry = HeapAccess.createRead(entry, base, field, cgNodeId);
							hreads.add(readEntry);
							HeapRead readExit = HeapAccess.createRead(exit, base, field, cgNodeId);
							hreads.add(readExit);
						}
					}
				}
			}
		}


		return hreads;
	}

	private Set<InstanceKeyOrigin> getPossibleAllocationSites(PDG<InstanceKey> pdg, int var) {
		Set<InstanceKeyOrigin> result = HashSetFactory.make();
		PointerKey pk = heap.getPointerKeyForLocal(pdg.getCallGraphNode(), var);
		OrdinalSet<InstanceKey> iks = pts.getPointsToSet(pk);
		for (InstanceKey ik : iks) {
			Set<InstanceKeyOrigin> sites = k2o.getOrigin(ik);
			if (sites != null && !sites.isEmpty()) {
				result.addAll(sites);
			}
		}

		return result;
	}

	private boolean mayBeEscaping(PDG<InstanceKey> pdg, SSAPutInstruction set) {
		if (escape != null && !set.isStatic()) {
			//TODO add write only if it possibly escapes the thread.run
			// method
			boolean escapes = false;
			Set<InstanceKeyOrigin> sites = getPossibleAllocationSites(pdg, set.getRef());
			for (CGNode cgStart : threadStarts) {
				for (InstanceKeyOrigin site : sites) {
					if (site.getCounter() != null && site.getNode() != null) {
						try {
							escapes |= escape.mayEscape(site.getNode(), site.getCounter().getProgramCounter(), cgStart);
						} catch (WalaException exc) {
							Log.warn(exc);
							escapes = true;
						}
					}

					if (escapes) {
						break;
					}
				}

				if (escapes) {
					break;
				}
			}

			return escapes;
		} else {
			return true;
		}
	}

	private Set<NormalStatement> findSets(PDG<InstanceKey> pdg) {
		Set<NormalStatement> gets = HashSetFactory.make();

		for (Statement stat : pdg) {
			if (stat.getKind() == Kind.NORMAL) {
				NormalStatement ns = (NormalStatement) stat;
				SSAInstruction ssa = ns.getInstruction();
				if (ssa instanceof SSAPutInstruction) {
					gets.add(ns);
				}
			}
		}

		return gets;
	}

	private Set<NormalStatement> findArraySets(PDG<InstanceKey> pdg) {
		Set<NormalStatement> gets = HashSetFactory.make();

		for (Statement stat : pdg) {
			if (stat.getKind() == Kind.NORMAL) {
				NormalStatement ns = (NormalStatement) stat;
				SSAInstruction ssa = ns.getInstruction();
				if (ssa instanceof SSAArrayStoreInstruction) {
					gets.add(ns);
				}
			}
		}

		return gets;
	}

	private Set<HeapWrite> getHeapWrites(PDG<InstanceKey> pdg) throws WalaException {
		Set<HeapWrite> hwrites = HashSetFactory.make();

		final CGNode cgNode = pdg.getCallGraphNode();
		final IClassHierarchy hierarchy = cg.getClassHierarchy();
		final int cgNodeId = cg.getNumber(cgNode);

		for (NormalStatement stat : findSets(pdg)) {
			SSAPutInstruction set = (SSAPutInstruction) stat.getInstruction();
			if (isThisPointerAccessInConstructor(cgNode.getMethod(), set)) {
				continue;
			}

			if (!mayBeEscaping(pdg, set)) {
				// skip writes to non-escaping objects
				continue;
			}

			OrdinalSet<InstanceKey> base;
			if (set.isStatic()) {
				base = OrdinalSet.empty();
			} else {
				if (set.getRef() < 0) {
					// skip operations with illegal value numbers
					Log.warn("Skipping instruction because reference value number < 0: " + set);
					continue;
				}

				PointerKey pkBase = heap.getPointerKeyForLocal(cgNode, set.getRef());;
				base = pts.getPointsToSet(pkBase);
			}

			IField ifield = hierarchy.resolveField(set.getDeclaredField());
			if (ifield == null) {
				// skip not resolvable fields;
				continue;
			}

			ParameterField field = ParameterFieldFactory.getFactory().getObjectField(ifield);
//			Set<PointerKey> pk = HashSetFactory.make();
//			if (set.isStatic()) {
//				pk.add(heap.getPointerKeyForStaticField(ifield));
//			} else {
//				for (InstanceKey ikBase : base) {
//					PointerKey pkField = heap.getPointerKeyForInstanceField(ikBase, ifield);
//					pk.add(pkField);
//				}
//			}

			HeapWrite write = HeapAccess.createWrite(stat, base, field, cgNodeId);
			hwrites.add(write);
		}

		if (DO_ARRAYS) {
			for (NormalStatement stat : findArraySets(pdg)) {
				SSAArrayStoreInstruction set = (SSAArrayStoreInstruction) stat.getInstruction();
				if (isThisPointerAccessInConstructor(cgNode.getMethod(), set)) {
					continue;
				}

				PointerKey pkBase = heap.getPointerKeyForLocal(cgNode, set.getArrayRef());
				OrdinalSet<InstanceKey> base = pts.getPointsToSet(pkBase);
				TypeReference tRef = set.getElementType();
				ParameterField field = ParameterFieldFactory.getFactory().getArrayField(tRef);

//				Set<PointerKey> pk = HashSetFactory.make();
//				for (InstanceKey ikBase : base) {
//					PointerKey pkField = heap.getPointerKeyForArrayContents(ikBase);
//					pk.add(pkField);
//				}

				HeapWrite write = HeapAccess.createWrite(stat, base, field, cgNodeId);
				hwrites.add(write);
			}
		}

		// do sync statements
		if (DO_SYNC) {
			for (NormalStatement stat : findSyncs(pdg)) {
				SSAMonitorInstruction sync = (SSAMonitorInstruction) stat.getInstruction();

				final int valNum = sync.getRef();
				PointerKey pkBase = heap.getPointerKeyForLocal(cgNode, valNum);
				OrdinalSet<InstanceKey> base = pts.getPointsToSet(pkBase);
				ParameterField field = ParameterFieldFactory.getFactory().getLockField();

				HeapWrite write = HeapAccess.createWrite(stat, base, field, cgNodeId);
				hwrites.add(write);
			}

			if (DO_SYNC_METHODS) {
				final IMethod im = cgNode.getMethod();
				if (im.isSynchronized()) {
					final Statement entry = findEntryNode(pdg);
					final Statement exit = findExitNode(pdg);

					if (!im.isStatic()) {
						int valNum = cgNode.getIR().getParameter(THIS_PARAM_NUM);
						PointerKey pks = pts.getHeapModel().getPointerKeyForLocal(cgNode, valNum);
						OrdinalSet<InstanceKey> base = pts.getPointsToSet(pks);
						ParameterField field = ParameterFieldFactory.getFactory().getLockField();

						HeapWrite writeEntry = HeapAccess.createWrite(entry, base, field, cgNodeId);
						hwrites.add(writeEntry);
						HeapWrite writeExit = HeapAccess.createWrite(exit, base, field, cgNodeId);
						hwrites.add(writeExit);
					} else {
						// search for static field class
						IClass cls = im.getDeclaringClass();
						IField classField = null;
						for (IField field : cls.getAllStaticFields()) {
							if (TypeReference.JavaLangClass.getName().equals(field.getFieldTypeReference().getName())) {
								if (field.getName().toString().startsWith(CLASS_FIELD_NAME_PREFIX)) {
									classField = field;
									break;
								}
							}
						}

						if (classField != null) {
							PointerKey pk = pts.getHeapModel().getPointerKeyForStaticField(classField);
							OrdinalSet<InstanceKey> base = pts.getPointsToSet(pk);
							ParameterField field = ParameterFieldFactory.getFactory().getLockField();

							HeapWrite writeEntry = HeapAccess.createWrite(entry, base, field, cgNodeId);
							hwrites.add(writeEntry);
							HeapWrite writeExit = HeapAccess.createWrite(exit, base, field, cgNodeId);
							hwrites.add(writeExit);
						} else {
							// no class field found - so we create an artificial one. this is some sort of HACK as
							// the WALA points-to analysis does not know about this field...
							OrdinalSet<InstanceKey> base = pts.getArtificialClassFieldPts(cls);
							ParameterField field = ParameterFieldFactory.getFactory().getClassLockField();

							HeapWrite writeEntry = HeapAccess.createWrite(entry, base, field, cgNodeId);
							hwrites.add(writeEntry);
							HeapWrite writeExit = HeapAccess.createWrite(exit, base, field, cgNodeId);
							hwrites.add(writeExit);
						}
					}
				}
			}
		}

		return hwrites;
	}

	private static abstract class HeapAccess {
		private final Statement expr;
		private final OrdinalSet<InstanceKey> base;
		private final ParameterField field;
		private final int cgNodeId;

		private HeapAccess(Statement expr, OrdinalSet<InstanceKey> base,
				ParameterField field, int cgNodeId) {
			this.expr = expr;
			this.base = base;
			this.field = field;
			this.cgNodeId = cgNodeId;
		}

		public static HeapWrite createWrite(Statement expr,
				OrdinalSet<InstanceKey> base, ParameterField field,
				int cgNodeId) {
			HeapWrite hw = new HeapWrite(expr, base, field, cgNodeId);
			return hw;
		}

		public static HeapRead createRead(Statement expr,
				OrdinalSet<InstanceKey> base, ParameterField field,
				int cgNodeId) {
			HeapRead hr = new HeapRead(expr, base, field, cgNodeId);
			return hr;
		}

		public abstract boolean isWrite();

		public int getPdgId() {
			return cgNodeId;
		}

		public Statement getExpr() {
			return expr;
		}

		public boolean isAliasing(HeapAccess acc) {
			boolean emtpyBases = (base == null && acc.base == null) ||
				(base != null && acc.base != null && base.isEmpty() && acc.base.isEmpty());

			return (field == acc.field) &&
				(emtpyBases || (base != null && base.containsAny(acc.base)));
		}

	}

	private static final class HeapRead extends HeapAccess {

		private HeapRead(Statement expr, OrdinalSet<InstanceKey> base,
				ParameterField field, int cgNodeId) {
			super(expr, base, field, cgNodeId);
		}

		public boolean isWrite() {
			return false;
		}
	}

	private static final class HeapWrite extends HeapAccess {

		private HeapWrite(Statement expr, OrdinalSet<InstanceKey> base,
				ParameterField field, int cgNodeId) {
			super(expr, base, field, cgNodeId);
		}

		public boolean isWrite() {
			return true;
		}
	}

	private static MutableIntSet getFromMapSet(TIntObjectHashMap<MutableIntSet> map, int key) {
		MutableIntSet set = map.get(key);
		if (set == null) {
			set = IntSetUtil.make();
			map.put(key, set);
		}

		return set;
	}

	private static void addToMapSet(TIntObjectHashMap<MutableIntSet> map, int key, int value) {
		MutableIntSet set = map.get(key);
		if (set == null) {
			set = IntSetUtil.make();
			map.put(key, set);
		}
		set.add(value);
	}


	/**
	 * Annotate each pdg with the id of thread it may run in. For each entry
	 * to thread.start() two ids are created to model the possibility that
	 * this call may be reached more than once. It doesn't matter how often
	 * it will be called. The only information we are intrested in is if it may
	 * be called more than once (>1 instances at the same time) and therefore
	 * the thread may be able to interfere with itself. This is approximated
	 * by adding two ids, everytime we are unsure if multiple instances of the
	 * same thread may exist.
	 * @throws CancelException
	 */
	private void computeThreadIds(IProgressMonitor progress) throws CancelException {
		Log.info("Computing thread ids");

		final CGNode mainNode = cg.getFakeRootNode();

		threadStart = findThreadStart();

		Set<CGNode> entryPoints = getListOfAllThreadEntryPoints(threadStart);

		// start enumeration of threads at 1 - main threads gets special number 0
		int currentThreadId = 1;
		for (CGNode threadRun : entryPoints) {
			final int pdgId = cg.getNumber(threadRun);
			IntSet transitiveCalled = getTransitiveCallsFromSameThread(threadRun, threadStart);

			final IntSet threadId;
			if (mainNode.equals(threadRun)) {
				// theres definitely only one main thread, so a single id is sufficent
				threadId = SparseIntSet.singleton(0);
				// put  static initializers (as approximation) in the same
				// thread as the main methods - this is not needed for wala, as wala has a
				// special fakeroot method that calls main and all clinits.
//				MutableIntSet clinitIds = getClinitsIntSet();
//				clinitIds.addAll(transitiveCalled);
//				transitiveCalled = clinitIds;
			} else {
				// for now we assume that every thread may have multiple instances
				threadId = SparseIntSet.pair(currentThreadId, currentThreadId + 1);
				currentThreadId += 2;
			}

			threads.put(pdgId, transitiveCalled);
			threadIds.put(pdgId, threadId);

			// set thread id for all pdgs that are part of this thread
			transitiveCalled.foreach(new IntSetAction() {

				public void act(int x) {
					for (IntIterator it = threadId.intIterator(); it.hasNext();) {
						addToMapSet(pdgId2threadIds, x, it.next());
					}
				}

			});

			if (progress.isCanceled()) {
				throw CancelException.make("Copmuting thread ids canceled.");
			}

			progress.worked(1);
		}
	}

	private Set<CGNode> getListOfAllThreadEntryPoints(IMethod threadStart) {
		//TODO make it more precise! The possible targets of Thread.start are
		// somehow very conservative. We only want those whose start() has been
		// triggered.
		Set<CGNode> entryPoints = HashSetFactory.make();

		/* when no thread start method is in the sdg it simply means that thread.start
		 * is never called -> so no threads except the main thread exists
		 *
		 * We search for all successors of the method threadStart (Thread.start())
		 * in the callgraph. These are the possible entrypoints of the controlflow
		 * of a new thread.
		 */
		if (threadStart != null && threadStarts == null) {
			threadStarts = HashSetFactory.make();
			Set<CGNode> startNodes = cg.getNodes(threadStart.getReference());
			for (CGNode start : startNodes) {
				if (start.getMethod().equals(threadStart)) {
					threadStarts.add(start);
				}
			}

			for (CGNode cgThread : threadStarts) {
				// what has been called by thread.start
				for (Iterator<? extends CGNode> it = cg.getSuccNodes(cgThread); it.hasNext();) {
					CGNode callee = it.next();
					entryPoints.add(callee);
				}
			}
		}

		// add main to runnables - this thread is always there:
		// fakerootnode contains call to main and to all clinits -> so we do not have to treat
		// clinits seperately
		CGNode cgMain = cg.getFakeRootNode();

		entryPoints.add(cgMain);

		return entryPoints;
	}

	public static boolean isThreadStart(IMethod method) {
		return "java.lang.Thread.start()V".equals(method.getSignature());
	}

	public static boolean isThreadRun(IMethod method) {
		return "java.lang.Thread.run()V".equals(method.getSignature());
	}

	private IMethod findThreadStart() {
		//first we search for the thread.start method in our sdg
		IMethod thread = null;

		TypeReference tRefThread =
			TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/Thread");

		IClass threadClass = cg.getClassHierarchy().lookupClass(tRefThread);
		if (threadClass != null) {
			for (IMethod method : threadClass.getDeclaredMethods()) {
				if (isThreadStart(method)) {
					if (thread == null) {
						thread = method;
					} else {
						Log.error("Found another method implementing Thread.start() - this is weird.");
						Log.error("Version 1: " + thread);
						Log.error("Version 2: " + method);
					}
				}
			}
		} else {
			Log.info("No java.lang.Thread class has been found.");
		}

		if (thread == null) {
			Log.info("No reference to Thread.start() has been found in the analyzed program.");
		} else {
			Log.info("Thread.start() has been found. Starting interference analysis.");
		}

		return thread;
	}

	/**
	 * Computes a set of all pdgs whose method may be called subsequently by the method
	 * provided as threadRun. It stops traversation when a new thread is created.
	 * The result is stored in an int set where each entry is
	 * the pdg-id of a pdg that may be called.
	 * @param threadRun
	 * @return intset of pdg ids
	 */
	private IntSet getTransitiveCallsFromSameThread(CGNode threadRun, final IMethod threadStart) {
		MutableIntSet called = new BitVectorIntSet();

		called.add(cg.getNumber(threadRun));

		searchCalleesSameThread(threadRun, called, threadStart);

		return called;
	}

	private void searchCalleesSameThread(CGNode caller, MutableIntSet called, final IMethod threadStart) {
		for (Iterator<? extends CGNode> it = cg.getSuccNodes(caller); it.hasNext();) {
			CGNode node = it.next();
			int pdgCurId = cg.getNumber(node);

			if (!called.contains(pdgCurId)) {
				called.add(pdgCurId);
				// only look further if no new thread has been created
				if (node.getMethod() != threadStart) {
					searchCalleesSameThread(node, called, threadStart);
				}
			}
		}
	}

}
