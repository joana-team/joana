/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.interference;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.escape.TrivialMethodEscape;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.SparseIntSet;

import edu.kit.joana.deprecated.jsdg.Messages;
import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.EntryNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ExpressionNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterFieldFactory;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.SyncNode;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.IPointerAnalysis;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.PointsToWrapper;
import edu.kit.joana.deprecated.jsdg.util.Debug;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.deprecated.jsdg.wala.SourceLocation;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.IKey2Origin;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.IKey2Origin.InstanceKeyOrigin;
import edu.kit.joana.wala.util.MultiMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Interference computation for the standard jSDG.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class InterferenceComputation {

	private final SDG sdg;
	private final MultiMap<IMethod, CGNode, Set<CGNode>> method2cgnode;
	private final CallGraph cg;
	private final Set<PDG> clinits;
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

	/**
	 * Computes interference dependence for multi-threaded programs
	 * corresponds to joana.SDG.computeInterference @214
	 * @throws CancelException
	 * @throws WalaException
	 */
	public static void computeInterference(SDG sdg, CallGraph cg, Set<PDG> clinits,
			MultiMap<IMethod, CGNode, Set<CGNode>> method2cgnode, boolean optimizeThisAccess, boolean useEscapeAnalysis,
			boolean ignoreClinits, HeapGraph hg, IKey2Origin k2o, IProgressMonitor progress) throws CancelException, WalaException {
		progress.subTask(Messages.getString("SDG.SubTask_Interference_Dep")); //$NON-NLS-1$
		Log.info("Computing thread interference");

		final InterferenceComputation ifcomp;

		if (ignoreClinits) {
			ifcomp = new InterferenceComputation(sdg, cg, null, method2cgnode, optimizeThisAccess, useEscapeAnalysis, hg, k2o);
		} else {
			ifcomp = new InterferenceComputation(sdg, cg, clinits, method2cgnode, optimizeThisAccess, useEscapeAnalysis, hg, k2o);
		}

		ifcomp.compute(progress);

		Log.info("Thread interference done.");

		progress.done();
	}

	private InterferenceComputation(SDG sdg, CallGraph cg, Set<PDG> clinits,
			MultiMap<IMethod, CGNode, Set<CGNode>> method2cgnode,
			boolean optimizeThisAccess, boolean useEscapeAnalysis,
			HeapGraph hg, IKey2Origin k2o) {
		this.sdg = sdg;
		this.cg = cg;
		this.k2o = k2o;
		this.method2cgnode = method2cgnode;
		this.clinits = clinits;
		this.threadIds = new TIntObjectHashMap<IntSet>();
		this.threads = new TIntObjectHashMap<IntSet>();
		this.optimizeThisAccess = optimizeThisAccess;

		//TODO here may be inserted a demand driven pointer analysis
		//this.pts = new PointsToWrapper(SDGFactory.pts, hg.getPointerAnalysis());
		this.pts = new PointsToWrapper(null, hg.getPointerAnalysis());

		if (useEscapeAnalysis) {
			escape = new TrivialMethodEscape(cg, hg);
		} else {
			escape = null;
		}
	}

	private void compute(IProgressMonitor progress) throws CancelException, WalaException {
		computeThreadIds(progress);

		if (Debug.Var.PRINT_THREADS.isSet()) {
			Util.printThreads(threads, threadIds, sdg);
		}

		if (threadStarts != null) {
			computeInterference(progress);
		}
	}

	/**
	 * Look for each heap accessing node which other heap accessing node may
	 * happen in parallel and may be referring to the same location.
	 * @throws CancelException
	 * @throws WalaException
	 */
	private final void computeInterference(IProgressMonitor progress) throws CancelException, WalaException {
		Log.info("Computing read-write/write-write interference for threads");

		for (PDG pdg : sdg.getAllContainedPDGs()) {
			if (pdg == null) {
				continue;
			}

			Set<HeapWrite> writes = getHeapWrites(pdg);

			if (writes.isEmpty()) {
				// a pdg without heap access does not interfere with anything
				// the interferences thay may happen because of read statements
				// only appear when there is a aliasing wirte statement in another
				// pdg. We skip them here because they will be added later on
				// while handling the pdg with the intefereing write.
				continue;
			}

			for (PDG pdgCur : sdg.getAllContainedPDGs()) {
				if (pdgCur == null) {
					continue;
				}

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

	private final void computeInterference(Set<HeapWrite> writes1,
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

	private final void addReadWriteInterference(HeapWrite write, HeapRead read) {
		AbstractPDGNode ewrite = write.getNode();
		PDG pdgWrite = sdg.getPdgForId(ewrite.getPdgId());
		AbstractPDGNode eread = read.getNode();

		if (!pdgWrite.containsNode(eread)) {
			pdgWrite.addNode(eread);
		}
		pdgWrite.addReadWriteInterference(ewrite, eread);

		if (Debug.Var.PRINT_THREAD_INTERFERENCES.isSet()) {
			PDG pdgRead = sdg.getPdgForId(eread.getPdgId());
			Log.info("Read-write: " + pdgWrite + "{" + ewrite + "} - " + pdgRead + "{" + eread + "}");
			SourceLocation locWrite = pdgWrite.getLocation(ewrite);
			SourceLocation locRead = pdgRead.getLocation(eread);
			Log.info("Read-write[loc]: " + locWrite + " - " + locRead);
			Log.info("Read-write[hash]: " + write + " - " + read);
		}
	}

	private final void addWriteWriteInterference(HeapWrite write1, HeapWrite write2) {
		AbstractPDGNode ewrite1 = write1.getNode();
		PDG pdgWrite1 = sdg.getPdgForId(ewrite1.getPdgId());
		AbstractPDGNode ewrite2 = write2.getNode();

		if (!pdgWrite1.containsNode(ewrite2)) {
			pdgWrite1.addNode(ewrite2);
		}
		pdgWrite1.addWriteWriteInterference(ewrite1, ewrite2);

		if (Debug.Var.PRINT_THREAD_INTERFERENCES.isSet()) {
			PDG pdgWrite2 = sdg.getPdgForId(ewrite2.getPdgId());
			Log.info("Write-write: " + pdgWrite1 + "{" + ewrite1 + "} - " + pdgWrite2 + "{" + ewrite2 + "}");
			SourceLocation locWrite1 = pdgWrite1.getLocation(ewrite1);
			SourceLocation locWrite2 = pdgWrite2.getLocation(ewrite2);
			Log.info("Write-write[loc]: " + locWrite1 + " - " + locWrite2);
		}
	}

	private final static boolean mayRunInParallelThreads(PDG pdg1, PDG pdg2) {
		IntSet tids1 = pdg1.getThreadIds();
		IntSet tids2 = pdg2.getThreadIds();
		// when thread ids are empty the pdg runs in no thread -> so it definitely
		// does not run in parallel
		// it also definitely does not run in parallel when both pdgs may only be
		// executed in a single thread and this thread is the same for both pdgs
		return !(tids1.isEmpty() || tids2.isEmpty() ||
			(tids1.size() == 1 && tids2.size() == 1 && tids1.sameValue(tids2)));
	}

	private final boolean isThisPointerAccessInConstructor(IMethod method, SSAFieldAccessInstruction instr) {
		if (!optimizeThisAccess) {
			return false;
		}

		if (method.isInit()) {
			// number 1 is the magic value number of the this pointer
			return (!instr.isStatic() && instr.getRef() == 1);
		}

		return false;
	}

	private final boolean isThisPointerAccessInConstructor(IMethod method, SSAArrayReferenceInstruction instr) {
		if (!optimizeThisAccess) {
			return false;
		}

		if (method.isInit()) {
			// number 1 is the magic value number of the this pointer
			return (instr.getArrayRef() == 1);
		}

		return false;
	}

	private final Set<HeapRead> getHeapReads(PDG pdg) {
		Set<HeapRead> hreads = HashSetFactory.make();

		for (SSAGetInstruction get : pdg.getGets()) {
			if (isThisPointerAccessInConstructor(pdg.getMethod(), get)) {
				continue;
			}

			List<AbstractPDGNode> nodes = pdg.getNodesForInstruction(get);

			assert (nodes != null);
			assert (nodes.size() == 1) : "More then one node found for a field access operation";
			assert (nodes.get(0) instanceof ExpressionNode) : "Field access node is not an expression: " + nodes.get(0);

			ExpressionNode expr = (ExpressionNode) nodes.get(0);
			OrdinalSet<InstanceKey> base;
			if (get.isStatic()) {
				base = OrdinalSet.empty();
			} else {
				if (get.getRef() < 0) {
					// skip operations with illegal value numbers
					Log.warn("Skipping instruction because reference value number < 0: " + get);
					continue;
				}
				PointerKey pk = pdg.getPointerKey(get.getRef());
				base = pts.getPointsToSet(pk);
			}

			IField ifield = pdg.getHierarchy().resolveField(get.getDeclaredField());
			if (ifield == null) {
				// skip not resolvable fields;
				continue;
			}
			ParameterField field = ParameterFieldFactory.getFactory().getObjectField(ifield);
//			Set<PointerKey> pk;
//			if (get.isStatic()) {
//				pk = HashSetFactory.make();
//				pk.add(pdg.getPointerKey(field));
//			} else {
//				// the pointerkey for the ssa var that points to the field content is not ref but def!
//				pk = pdg.getPointerKeys(get.getDef());
//			}

			HeapRead read = HeapAccess.createRead(expr, base, field);
			hreads.add(read);
		}

		if (DO_ARRAYS) {
			for (SSAArrayLoadInstruction get : pdg.getArrayGets()) {
				if (isThisPointerAccessInConstructor(pdg.getMethod(), get)) {
					continue;
				}

				List<AbstractPDGNode> nodes = pdg.getNodesForInstruction(get);

				assert (nodes != null);
				assert (nodes.size() == 1) : "More then one node found for a field access operation";
				assert (nodes.get(0) instanceof ExpressionNode) : "Field access node is not an expression: " + nodes.get(0);

				ExpressionNode expr = (ExpressionNode) nodes.get(0);
				PointerKey pk = pdg.getPointerKey(get.getArrayRef());
				OrdinalSet<InstanceKey> base = pts.getPointsToSet(pk);
				TypeReference tRef = get.getElementType();
				ParameterField field = ParameterFieldFactory.getFactory().getArrayField(tRef);

				HeapRead read = HeapAccess.createRead(expr, base, field);
				hreads.add(read);
			}
		}

		// do sync statements
		if (DO_SYNC) {
			for (AbstractPDGNode node : pdg) {
				if (!node.isParameterNode() && node.getPdgId() == pdg.getId() && node instanceof SyncNode) {
					SSAInstruction instr = pdg.getInstructionForNode(node);

					assert (instr != null);
					assert (instr instanceof SSAMonitorInstruction);

					int valNum = ((SSAMonitorInstruction) instr).getRef();
					PointerKey pk = pdg.getPointerKey(valNum);
					OrdinalSet<InstanceKey> base = pts.getPointsToSet(pk);
					ParameterField field = ParameterFieldFactory.getFactory().getLockField();

					HeapRead read = HeapAccess.createRead((SyncNode) node, base, field);
					hreads.add(read);
				}
			}

			if (DO_SYNC_METHODS) {
				final IMethod im = pdg.getMethod();
				if (im.isSynchronized()) {
					if (!im.isStatic()) {
						int valNum = pdg.getIR().getParameter(THIS_PARAM_NUM);
						PointerKey pk = pdg.getPointerKey(valNum);
						OrdinalSet<InstanceKey> base = pts.getPointsToSet(pk);
						ParameterField field = ParameterFieldFactory.getFactory().getLockField();

						HeapRead readEntry = HeapAccess.createRead(pdg.getRoot(), base, field);
						hreads.add(readEntry);
						HeapRead readExit = HeapAccess.createRead(pdg.getExit(), base, field);
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

							HeapRead readEntry = HeapAccess.createRead(pdg.getRoot(), base, field);
							hreads.add(readEntry);
							HeapRead readExit = HeapAccess.createRead(pdg.getExit(), base, field);
							hreads.add(readExit);
						} else {
							// no class field found - so we create an artificial one. this is some sort of HACK as
							// the WALA points-to analysis does not know about this field...
							OrdinalSet<InstanceKey> base = pts.getArtificialClassFieldPts(cls);
							ParameterField field = ParameterFieldFactory.getFactory().getClassLockField();

							HeapRead readEntry = HeapAccess.createRead(pdg.getRoot(), base, field);
							hreads.add(readEntry);
							HeapRead readExit = HeapAccess.createRead(pdg.getExit(), base, field);
							hreads.add(readExit);
						}
					}
				}
			}
		}

		return hreads;
	}

	private final Set<InstanceKeyOrigin> getPossibleAllocationSites(PDG pdg, int var) {
		Set<InstanceKeyOrigin> result = HashSetFactory.make();
		PointerKey pk = pdg.getPointerKey(var);
		OrdinalSet<InstanceKey> iks = pts.getPointsToSet(pk);
		for (InstanceKey ik : iks) {
			Set<InstanceKeyOrigin> sites = k2o.getOrigin(ik);
			if (sites != null && !sites.isEmpty()) {
				result.addAll(sites);
			}
		}

		return result;
	}

	private final boolean mayBeEscaping(PDG pdg, SSAPutInstruction set) {
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

	private final Set<HeapWrite> getHeapWrites(PDG pdg) throws WalaException {
		Set<HeapWrite> hwrites = HashSetFactory.make();

		for (SSAPutInstruction set : pdg.getSets()) {
			if (isThisPointerAccessInConstructor(pdg.getMethod(), set)) {
				continue;
			}

			if (!mayBeEscaping(pdg, set)) {
				// skip writes to non-escaping objects
				continue;
			}

			List<AbstractPDGNode> nodes = pdg.getNodesForInstruction(set);

			assert (nodes != null);
			assert (nodes.size() == 1) : "More then one node found for a field access operation";
			assert (nodes.get(0) instanceof ExpressionNode) : "Field access node is not an expression: " + nodes.get(0);

			ExpressionNode expr = (ExpressionNode) nodes.get(0);
			OrdinalSet<InstanceKey> base;
			if (set.isStatic()) {
				base = OrdinalSet.empty();
			} else {
				if (set.getRef() < 0) {
					// skip operations with illegal value numbers
					Log.warn("Skipping instruction because reference value number < 0: " + set);
					continue;
				}

				PointerKey pk = pdg.getPointerKey(set.getRef());
				base = pts.getPointsToSet(pk);
			}

			IField ifield = pdg.getHierarchy().resolveField(set.getDeclaredField());
			if (ifield == null) {
				// skip not resolvable fields;
				continue;
			}

			ParameterField field = ParameterFieldFactory.getFactory().getObjectField(ifield);
//			Set<PointerKey> pk;
//			if (set.isStatic()) {
//				pk = HashSetFactory.make();
//				pk.add(pdg.getPointerKey(field));
//			} else {
//				pk = pdg.getPointerKeys(set.getRef());
//			}

			HeapWrite write = HeapAccess.createWrite(expr, base, field);
			hwrites.add(write);
		}

		if (DO_ARRAYS) {
			for (SSAArrayStoreInstruction set : pdg.getArraySets()) {
				if (isThisPointerAccessInConstructor(pdg.getMethod(), set)) {
					continue;
				}

				List<AbstractPDGNode> nodes = pdg.getNodesForInstruction(set);

				assert (nodes != null);
				assert (nodes.size() == 1) : "More then one node found for a field access operation";
				assert (nodes.get(0) instanceof ExpressionNode) : "Field access node is not an expression: " + nodes.get(0);

				ExpressionNode expr = (ExpressionNode) nodes.get(0);
				PointerKey pk = pdg.getPointerKey(set.getArrayRef());
				OrdinalSet<InstanceKey> base = pts.getPointsToSet(pk);
				TypeReference tRef = set.getElementType();
				ParameterField field = ParameterFieldFactory.getFactory().getArrayField(tRef);

				HeapWrite write = HeapAccess.createWrite(expr, base, field);
				hwrites.add(write);
			}
		}

		// do sync statements
		if (DO_SYNC) {
			for (AbstractPDGNode node : pdg) {
				if (!node.isParameterNode() && node.getPdgId() == pdg.getId() && node instanceof SyncNode) {
					SSAInstruction instr = pdg.getInstructionForNode(node);

					assert (instr != null);
					assert (instr instanceof SSAMonitorInstruction);

					int valNum = ((SSAMonitorInstruction) instr).getRef();
					PointerKey pk = pdg.getPointerKey(valNum);
					OrdinalSet<InstanceKey> base = pts.getPointsToSet(pk);
					ParameterField field = ParameterFieldFactory.getFactory().getLockField();

					HeapWrite write = HeapAccess.createWrite((SyncNode) node, base, field);
					hwrites.add(write);
				}
			}

			if (DO_SYNC_METHODS) {
				final IMethod im = pdg.getMethod();
				if (im.isSynchronized()) {
					if (!im.isStatic()) {
						int valNum = pdg.getIR().getParameter(THIS_PARAM_NUM);
						PointerKey pk = pdg.getPointerKey(valNum);
						OrdinalSet<InstanceKey> base = pts.getPointsToSet(pk);
						ParameterField field = ParameterFieldFactory.getFactory().getLockField();

						HeapWrite writeEntry = HeapAccess.createWrite(pdg.getRoot(), base, field);
						hwrites.add(writeEntry);
						HeapWrite writeExit = HeapAccess.createWrite(pdg.getExit(), base, field);
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

							HeapWrite writeEntry = HeapAccess.createWrite(pdg.getRoot(), base, field);
							hwrites.add(writeEntry);
							HeapWrite writeExit = HeapAccess.createWrite(pdg.getExit(), base, field);
							hwrites.add(writeExit);
						} else {
							// no class field found - so we create an artificial one. this is some sort of HACK as
							// the WALA points-to analysis does not know about this field...
							OrdinalSet<InstanceKey> base = pts.getArtificialClassFieldPts(cls);
							ParameterField field = ParameterFieldFactory.getFactory().getClassLockField();

							HeapWrite writeEntry = HeapAccess.createWrite(pdg.getRoot(), base, field);
							hwrites.add(writeEntry);
							HeapWrite writeExit = HeapAccess.createWrite(pdg.getExit(), base, field);
							hwrites.add(writeExit);
						}
					}
				}
			}
		}

		return hwrites;
	}

	private static abstract class HeapAccess {
		private final AbstractPDGNode expr;
		private final OrdinalSet<InstanceKey> base;
		private final ParameterField field;

		private HeapAccess(AbstractPDGNode expr, OrdinalSet<InstanceKey> base,
				ParameterField field) {
			this.expr = expr;
			this.base = base;
			this.field = field;
		}

		public static HeapWrite createWrite(ExpressionNode expr,
				OrdinalSet<InstanceKey> base, ParameterField field) {
			HeapWrite hw = new HeapWrite(expr, base, field);
			return hw;
		}

		public static HeapWrite createWrite(EntryNode expr,
				OrdinalSet<InstanceKey> base, ParameterField field) {
			HeapWrite hw = new HeapWrite(expr, base, field);
			return hw;
		}

		public static HeapWrite createWrite(AbstractParameterNode expr,
				OrdinalSet<InstanceKey> base, ParameterField field) {
			HeapWrite hw = new HeapWrite(expr, base, field);
			return hw;
		}

		public static HeapRead createRead(ExpressionNode expr,
				OrdinalSet<InstanceKey> base, ParameterField field) {
			HeapRead hr = new HeapRead(expr, base, field);
			return hr;
		}

		public static HeapRead createRead(EntryNode expr,
				OrdinalSet<InstanceKey> base, ParameterField field) {
			HeapRead hr = new HeapRead(expr, base, field);
			return hr;
		}

		public static HeapRead createRead(AbstractParameterNode expr,
				OrdinalSet<InstanceKey> base, ParameterField field) {
			HeapRead hr = new HeapRead(expr, base, field);
			return hr;
		}

		public abstract boolean isWrite();

		public AbstractPDGNode getNode() {
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

		private HeapRead(AbstractPDGNode expr, OrdinalSet<InstanceKey> base,
				ParameterField field) {
			super(expr, base, field);
		}

		public boolean isWrite() {
			return false;
		}
	}

	private static final class HeapWrite extends HeapAccess {

		private HeapWrite(AbstractPDGNode expr, OrdinalSet<InstanceKey> base,
				ParameterField field) {
			super(expr, base, field);
		}

		public boolean isWrite() {
			return true;
		}
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
	private final void computeThreadIds(IProgressMonitor progress) throws CancelException {
		Log.info("Computing thread ids");

		threadStart = findThreadStart();

		Set<CGNode> entryPoints = getListOfAllThreadEntryPoints(threadStart);

		// start enumeration of threads at 1 - main threads gets special number 0
		int currentThreadId = 1;
		for (CGNode threadRun : entryPoints) {
			PDG pdg = sdg.getPdgForMethodSignature(threadRun);
			IntSet transitiveCalled = getTransitiveCallsFromSameThread(threadRun, threadStart);

			final IntSet threadId;
			if (pdg.getMethod() == sdg.getMain()) {
				// theres definitely only one main thread, so a single id is sufficent
				threadId = SparseIntSet.singleton(0);
				// put  static initializers (as approximation) in the same
				// thread as the main methods
				MutableIntSet clinitIds = getClinitsIntSet();
				clinitIds.addAll(transitiveCalled);
				transitiveCalled = clinitIds;
			} else {
				// for now we assume that every thread may have multiple instances
				threadId = SparseIntSet.pair(currentThreadId, currentThreadId + 1);
				currentThreadId += 2;
			}

			threads.put(pdg.getId(), transitiveCalled);
			threadIds.put(pdg.getId(), threadId);

			// set thread id for all pdgs that are part of this thread
			transitiveCalled.foreach(new IntSetAction() {

				public void act(int x) {
					PDG pdg = sdg.getPdgForId(x);
					for (IntIterator it = threadId.intIterator(); it.hasNext();) {
						pdg.addThreadId(it.next());
					}
				}

			});

			if (progress.isCanceled()) {
				throw CancelException.make("Copmuting thread ids canceled.");
			}

			progress.worked(1);
		}
	}

	private final Set<CGNode> getListOfAllThreadEntryPoints(IMethod threadStart) {
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
			threadStarts = method2cgnode.get(threadStart);
			for (CGNode cgThread : threadStarts) {
				// what has been called by thread.start
				for (Iterator<? extends CGNode> it = cg.getSuccNodes(cgThread); it.hasNext();) {
					CGNode callee = it.next();
					if (overwritesThreadRun(callee)) {
						entryPoints.add(callee);
					} else {
						Log.info("Skipping call from Thread.start to " + callee);
					}
				}
			}
		}

		// add main to runnables - this thread is always there:
		Set<CGNode> cgMain = method2cgnode.get(sdg.getMain());

		assert (cgMain != null);
		assert (cgMain.size() == 1) : "More then one main method in callgraph - this is weird!: " + cgMain;

		entryPoints.addAll(cgMain);

		return entryPoints;
	}

	private boolean overwritesThreadRun(CGNode node) {
		IMethod method = node.getMethod();
		Selector sel = method.getSelector();
		if ("run()V".equals(sel.toString())) {
	        IClassHierarchy cha = cg.getClassHierarchy();
	        IClass throwable = cha.lookupClass(TypeReference.JavaLangThread);
	        IClass klass = method.getDeclaringClass();

	        return cha.isSubclassOf(klass, throwable);
		} else {
			return false;
		}
	}

	public static boolean isThreadStart(IMethod method) {
		return "java.lang.Thread.start()V".equals(method.getSignature());
	}

	public static boolean isThreadRun(IMethod method) {
		return "java.lang.Thread.run()V".equals(method.getSignature());
	}

	public static boolean maybeThreadRun(IMethod method) {
		return method.getSignature().endsWith(".run()V");
	}

	private final IMethod findThreadStart() {
		//first we search for the thread.start method in our sdg
		IMethod thread = null;
		for (IMethod method : method2cgnode.keySet()) {
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

		if (thread == null) {
			Log.info("No reference to Thread.start() has been found in the analyzed program.");
		} else {
			Log.info("Thread.start() has been found. Starting interference analysis.");
		}

		return thread;
	}

	private final MutableIntSet getClinitsIntSet() {
		MutableIntSet clinitIds = new BitVectorIntSet();

		if (clinits != null) {
			for (PDG clpdg : clinits) {
				clinitIds.add(clpdg.getId());
			}
		}

		return clinitIds;
	}

	/**
	 * Computes a set of all pdgs whose method may be called subsequently by the method
	 * provided as threadRun. It stops traversation when a new thread is created.
	 * The result is stored in an int set where each entry is
	 * the pdg-id of a pdg that may be called.
	 * @param threadRun
	 * @return intset of pdg ids
	 */
	private final IntSet getTransitiveCallsFromSameThread(CGNode threadRun, final IMethod threadStart) {
		MutableIntSet called = new BitVectorIntSet();

		PDG pdg = sdg.getPdgForMethodSignature(threadRun);
		called.add(pdg.getId());

		searchCalleesSameThread(threadRun, called, threadStart);

		return called;
	}

	private final void searchCalleesSameThread(CGNode caller, MutableIntSet called, final IMethod threadStart) {
		for (Iterator<CGNode> it = cg.getSuccNodes(caller); it.hasNext();) {
			CGNode node = it.next();
			PDG pdgCur = sdg.getPdgForMethodSignature(node);
			if (pdgCur != null && !called.contains(pdgCur.getId())) {
				called.add(pdgCur.getId());
				// only look further if no new thread has been created
				if (pdgCur.getMethod() != threadStart) {
					searchCalleesSameThread(node, called, threadStart);
				}
			}
		}
	}

}
