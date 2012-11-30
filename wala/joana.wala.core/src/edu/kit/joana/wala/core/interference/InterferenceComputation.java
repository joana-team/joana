/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.interference;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.SparseIntSet;

import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.core.EscapeAnalysis;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGField;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SourceLocation;
import gnu.trove.map.hash.TIntObjectHashMap;



/**
 * Interference computation for the standard jSDG.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class InterferenceComputation {

	private static final Logger debug = Log.getLogger(Log.L_WALA_INTERFERENCE_DEBUG);
	private static final boolean IS_DEBUG = debug.isEnabled();

	private final SDGBuilder builder;
	private final EscapeAnalysis escape;
	private final ThreadInformationProvider tiProvider;
	private Set<CGNode> threadStarts = null;
	private final boolean optimizeThisAccess;
	private final boolean ignoreClInits;
	private static final boolean DO_ARRAYS = true;
	private static final boolean DO_SYNC = true;
	private static final boolean DO_SYNC_METHODS = DO_SYNC && true;

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

	private final Map<PDG, MutableIntSet> threadIdsOfPDGs = new HashMap<PDG, MutableIntSet>();

	/**
	 * Computes interference dependence for multi-threaded programs
	 * corresponds to joana.SDG.computeInterference @214
	 * @throws CancelException
	 * @throws WalaException
	 */
	public static Set<InterferenceEdge> computeInterference(SDGBuilder builder, ThreadInformationProvider tiProvider,
			boolean optimizeThisAccess,
			boolean ignoreClinits, EscapeAnalysis escape, IProgressMonitor progress) throws CancelException {
		progress.subTask("Thread interference computation started..."); //$NON-NLS-1$
		if (IS_DEBUG) debug.outln("Computing thread interference");

		final InterferenceComputation ifcomp;


		ifcomp = new InterferenceComputation(builder, tiProvider, optimizeThisAccess, ignoreClinits, escape);

		Set<InterferenceEdge> ret = ifcomp.compute(progress);
		progress.done();
		if (IS_DEBUG) debug.outln("Thread interference done.");
		return ret;
	}

	private InterferenceComputation(SDGBuilder builder, ThreadInformationProvider tiProvider, boolean optimizeThisAccess, boolean ignoreClinits, EscapeAnalysis escape) {
		this.builder = builder;
		this.tiProvider = tiProvider;
		//this.cg = cg;
		//this.k2o = k2o;
		//this.method2cgnode = method2cgnode;
		this.threadIds = new TIntObjectHashMap<IntSet>();
		this.threads = new TIntObjectHashMap<IntSet>();
		this.optimizeThisAccess = optimizeThisAccess;
		this.ignoreClInits = ignoreClinits;

		//TODO here may be inserted a demand driven pointer analysis
		//this.pts = new PointsToWrapper(SDGFactory.pts, hg.getPointerAnalysis());
		this.escape = escape;
	}

	private Set<InterferenceEdge> compute(IProgressMonitor progress) throws CancelException {
		computeThreadIds(progress);

		//		if (Debug.Var.PRINT_THREADS.isSet()) {
		//			Util.printThreads(threads, threadIds, sdg);
		//		}

		if (tiProvider.getAllThreadStartNodesInCallGraph() != null) {
			return computeInterference(progress);
		} else {
			return null;
		}
	}

	/**
	 * Look for each heap accessing node which other heap accessing node may
	 * happen in parallel and may be referring to the same location.
	 * @throws CancelException
	 * @throws WalaException
	 */
	private final Set<InterferenceEdge> computeInterference(IProgressMonitor progress) throws CancelException {
		if (IS_DEBUG) debug.outln("Computing read-write/write-write interference for threads");
		Set<InterferenceEdge> ret = new HashSet<InterferenceEdge>();
		for (PDG pdg : getPDGs()) {
			if (pdg == null) {
				continue;
			}

			Set<HeapWrite> writes = getHeapWrites(pdg);

			if (writes.isEmpty()) {
				// a pdg without heap access does not interfere with anything
				// the interferences that may happen because of read statements
				// only appear when there is a aliasing write statement in another
				// pdg. We skip them here because they will be added later on
				// while handling the pdg with the interfering write.
				continue;
			}

			for (PDG pdgCur : getPDGs()) {
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

					ret.addAll(computeInterference(writes, readsCur, writesCur));
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

		return ret;
	}

	private Collection<PDG> getPDGs() {
		return builder.getAllPDGs();
	}

	private CallGraph getCallGraph() {
		return builder.getNonPrunedWalaCallGraph();
	}

	private final Set<InterferenceEdge> computeInterference(Set<HeapWrite> writes1,
			Set<HeapRead> reads2, Set<HeapWrite> writes2) {

		Set<InterferenceEdge> ret = new HashSet<InterferenceEdge>();
		for (HeapWrite write : writes1) {
			for (HeapRead read2 : reads2) {
				if (write.isAliasing(read2)) {
					ret.add(addReadWriteInterference(write, read2));
				}
			}
			for (HeapWrite write2 : writes2) {
				if (write.isAliasing(write2)) {
					ret.add(addWriteWriteInterference(write, write2));
				}
			}
		}

		return ret;
	}

	private final InterferenceEdge addReadWriteInterference(HeapWrite write, HeapRead read) {

		PDGNode ewrite = write.getNode();
		PDG pdgWrite = getPdgForId(ewrite.getPdgId());
		PDGNode eread = read.getNode();

		InterferenceEdge ret = InterferenceEdge.createReadWriteInterferenceEdge(pdgWrite, ewrite, eread);

		//		if (!pdgWrite.containsNode(eread)) {
		//			pdgWrite.addNode(eread);
		//		}

		//pdgWrite.addReadWriteInterference(ewrite, eread);

		if (IS_DEBUG) {
			PDG pdgRead = getPdgForId(eread.getPdgId());
			debug.outln("Read-write: " + pdgWrite + "{" + ewrite + "} - " + pdgRead + "{" + eread + "}");
			SourceLocation locWrite = ewrite.getSourceLocation();
			SourceLocation locRead = eread.getSourceLocation();
			debug.outln("Read-write[loc]: " + locWrite + " - " + locRead);
			debug.outln("Read-write[hash]: " + write + " - " + read);
		}

		return ret;
	}

	private PDG getPdgForId(int id) {
		return builder.getPDGforId(id);
	}

	private final InterferenceEdge addWriteWriteInterference(HeapWrite write1, HeapWrite write2) {
		PDGNode ewrite1 = write1.getNode();
		PDG pdgWrite1 = getPdgForId(ewrite1.getPdgId());
		PDGNode ewrite2 = write2.getNode();

		InterferenceEdge ret  = InterferenceEdge.createWriteWriteInterferenceEdge(pdgWrite1, ewrite1, ewrite2);

		//		if (!pdgWrite1.containsNode(ewrite2)) {
		//			pdgWrite1.addNode(ewrite2);
		//		}

		//pdgWrite1.addWriteWriteInterference(ewrite1, ewrite2);

		if (IS_DEBUG) {
			PDG pdgWrite2 = getPdgForId(ewrite2.getPdgId());
			debug.outln("Write-write: " + pdgWrite1 + "{" + ewrite1 + "} - " + pdgWrite2 + "{" + ewrite2 + "}");
			SourceLocation locWrite1 = ewrite1.getSourceLocation();
			SourceLocation locWrite2 = ewrite2.getSourceLocation();
			debug.outln("Write-write[loc]: " + locWrite1 + " - " + locWrite2);
		}

		return ret;
	}

	private IntSet getThreadIds(PDG pdg) {
		if (threadIdsOfPDGs.containsKey(pdg)) {
			return threadIdsOfPDGs.get(pdg);
		} else {
			return new EmptyIntSet();
		}
	}

	private final boolean mayRunInParallelThreads(PDG pdg1, PDG pdg2) {
		IntSet tids1 = getThreadIds(pdg1);
		IntSet tids2 = getThreadIds(pdg2);
		// when thread ids are empty the pdg runs in no thread -> so it definitely
		// does not run in parallel
		// it also definitely does not run in parallel when both pdgs may only be
		// executed in a single thread and this thread is the same for both pdgs
		return !(tids1.isEmpty() || tids2.isEmpty() ||
				(tids1.size() == 1 && tids2.size() == 1 && tids1.sameValue(tids2)));
	}

	private final boolean isThisPointerAccessInConstructor(IMethod method, SSAInstruction instr) {
		assert (instr instanceof SSAFieldAccessInstruction) || (instr instanceof SSAArrayReferenceInstruction);
		if (method.isInit()) {
			if (instr instanceof SSAFieldAccessInstruction) {
				SSAFieldAccessInstruction fInstr = (SSAFieldAccessInstruction)instr;
				// number 1 is the magic value number of the this pointer
				return (!fInstr.isStatic() && fInstr.getRef() == 1);
			} else if (instr instanceof SSAArrayReferenceInstruction) {
				SSAArrayReferenceInstruction aInstr = (SSAArrayReferenceInstruction)instr;
				// number 1 is the magic value number of the this pointer
				return (aInstr.getArrayRef() == 1);
			} else {
				throw new IllegalStateException("isThisPointerAccessInConstructor can only be called with SSAFieldAccessInstructions or SSAArrayReferenceInstructions!");
			}
		} else {
			return false;
		}
	}

	private final Set<HeapRead> getHeapReads(PDG pdg) {
		Set<HeapRead> hreads = HashSetFactory.make();
		for (PDGField fieldRead : pdg.getFieldReads()) {
			SSAInstruction i = pdg.getInstruction(fieldRead.node);
			OrdinalSet<InstanceKey> base;
			assert (i instanceof SSAGetInstruction || i instanceof SSAArrayLoadInstruction): i.getClass().toString();
			if (i instanceof SSAGetInstruction) {
				SSAGetInstruction getInstr = (SSAGetInstruction)i;

				if (optimizeThisAccess && isThisPointerAccessInConstructor(pdg.getMethod(), getInstr)) {
					continue;
				}

				base = getAccessDataFromInstruction(getInstr, pdg);
			} else if (i instanceof SSAArrayLoadInstruction && DO_ARRAYS) {
				SSAArrayLoadInstruction arrLoadInstr = (SSAArrayLoadInstruction)i;

				if (optimizeThisAccess && isThisPointerAccessInConstructor(pdg.getMethod(), arrLoadInstr)) {
					continue;
				}

				//				if (!mayBeEscaping(pdg, arrStoreInstr)) {
				//					continue;
				//				}

				base = getAccessDataFromInstruction(arrLoadInstr, pdg);
			} else {
				throw new IllegalStateException("pdg.getFieldReads() should only yield SSAGetInstructions or SSAArrayLoadInstructions!");
			}
			HeapRead read = HeapAccess.createRead(fieldRead.node, base, fieldRead.field);
			hreads.add(read);
		}

		// do sync statements
		//		if (DO_SYNC) {
		//			for (AbstractPDGNode node : pdg) {
		//				if (!node.isParameterNode() && node.getPdgId() == pdg.getId() && node instanceof SyncNode) {
		//					SSAInstruction instr = pdg.getInstructionForNode(node);
		//
		//					assert (instr != null);
		//					assert (instr instanceof SSAMonitorInstruction);
		//
		//					int valNum = ((SSAMonitorInstruction) instr).getRef();
		//					PointerKey pk = pdg.getPointerKey(valNum);
		//					OrdinalSet<InstanceKey> base = pts.getPointsToSet(pk);
		//					ParameterField field = ParameterFieldFactory.getFactory().getLockField();
		//
		//					HeapRead read = HeapAccess.createRead((SyncNode) node, base, field);
		//					hreads.add(read);
		//				}
		//			}

		//			if (DO_SYNC_METHODS) {
		//				final IMethod im = pdg.getMethod();
		//				if (im.isSynchronized()) {
		//					if (!im.isStatic()) {
		//						int valNum = pdg.getIR().getParameter(THIS_PARAM_NUM);
		//						PointerKey pk = pdg.getPointerKey(valNum);
		//						OrdinalSet<InstanceKey> base = pts.getPointsToSet(pk);
		//						ParameterField field = ParameterFieldFactory.getFactory().getLockField();
		//
		//						HeapRead readEntry = HeapAccess.createRead(pdg.getRoot(), base, field);
		//						hreads.add(readEntry);
		//						HeapRead readExit = HeapAccess.createRead(pdg.getExit(), base, field);
		//						hreads.add(readExit);
		//					} else {
		//						// search for static field class
		//						IClass cls = im.getDeclaringClass();
		//						IField classField = null;
		//						for (IField field : cls.getAllStaticFields()) {
		//							if (TypeReference.JavaLangClass.getName().equals(field.getFieldTypeReference().getName())) {
		//								if (field.getName().toString().startsWith(CLASS_FIELD_NAME_PREFIX)) {
		//									classField = field;
		//									break;
		//								}
		//							}
		//						}
		//
		//						if (classField != null) {
		//							PointerKey pk = pts.getHeapModel().getPointerKeyForStaticField(classField);
		//							OrdinalSet<InstanceKey> base = pts.getPointsToSet(pk);
		//							ParameterField field = ParameterFieldFactory.getFactory().getLockField();
		//
		//							HeapRead readEntry = HeapAccess.createRead(pdg.getRoot(), base, field);
		//							hreads.add(readEntry);
		//							HeapRead readExit = HeapAccess.createRead(pdg.getExit(), base, field);
		//							hreads.add(readExit);
		//						} else {
		//							// no class field found - so we create an artificial one. this is some sort of HACK as
		//							// the WALA points-to analysis does not know about this field...
		//							OrdinalSet<InstanceKey> base = pts.getArtificialClassFieldPts(cls);
		//							ParameterField field = ParameterFieldFactory.getFactory().getClassLockField();
		//
		//							HeapRead readEntry = HeapAccess.createRead(pdg.getRoot(), base, field);
		//							hreads.add(readEntry);
		//							HeapRead readExit = HeapAccess.createRead(pdg.getExit(), base, field);
		//							hreads.add(readExit);
		//						}
		//					}
		//				}
		//			}
		//		}

		return hreads;
	}

	final OrdinalSet<InstanceKey> getPointsToSet(PDG pdg, int ref) {
		PointerKey pk = builder.getPointerAnalysis().getHeapModel().getPointerKeyForLocal(pdg.cgNode, ref);
		return getPointerAnalysis().getPointsToSet(pk);
	}

	private PointerAnalysis getPointerAnalysis() {
		return builder.getPointerAnalysis();
	}

	private final boolean mayBeEscaping(PDG pdg, SSAPutInstruction set) {
		if (!set.isStatic() && escape != null) {
			OrdinalSet<InstanceKey> pts = getPointsToSet(pdg, set.getRef());
			return escape.mayEscape(pdg.cgNode, pts, tiProvider.getAllThreadStartNodesInCallGraph());
		} else {
			return true;
		}
	}

	private final Set<HeapWrite> getHeapWrites(PDG pdg) {
		Set<HeapWrite> hwrites = HashSetFactory.make();
		for (PDGField fieldWrite : pdg.getFieldWrites()) {
			SSAInstruction i = pdg.getInstruction(fieldWrite.node);
			OrdinalSet<InstanceKey> base;
			assert (i instanceof SSAPutInstruction || i instanceof SSAArrayStoreInstruction);
			if (i instanceof SSAPutInstruction) {
				SSAPutInstruction putInstr = (SSAPutInstruction)i;

				if (optimizeThisAccess && isThisPointerAccessInConstructor(pdg.getMethod(), putInstr)) {
					continue;
				}

				if (!mayBeEscaping(pdg, putInstr)) {
					continue;
				}

				base = getAccessDataFromInstruction(putInstr, pdg);
			} else if (i instanceof SSAArrayStoreInstruction && DO_ARRAYS) {
				SSAArrayStoreInstruction arrStoreInstr = (SSAArrayStoreInstruction)i;

				if (optimizeThisAccess && isThisPointerAccessInConstructor(pdg.getMethod(), arrStoreInstr)) {
					continue;
				}

				//				if (!mayBeEscaping(pdg, arrStoreInstr)) {
				//					continue;
				//				}

				base = getAccessDataFromInstruction(arrStoreInstr, pdg);
			} else {
				throw new IllegalStateException("pdg.getFieldWrites() should only yield SSAPutInstructions or SSAArrayStoreInstructions!");
			}
			HeapWrite write = HeapAccess.createWrite(fieldWrite.node, base, fieldWrite.field);
			hwrites.add(write);
		}

		//		// do sync statements
		//		if (DO_SYNC) {
		//			for (AbstractPDGNode node : pdg) {
		//				if (!node.isParameterNode() && node.getPdgId() == pdg.getId() && node instanceof SyncNode) {
		//					SSAInstruction instr = pdg.getInstructionForNode(node);
		//
		//					assert (instr != null);
		//					assert (instr instanceof SSAMonitorInstruction);
		//
		//					int valNum = ((SSAMonitorInstruction) instr).getRef();
		//					PointerKey pk = pdg.getPointerKey(valNum);
		//					OrdinalSet<InstanceKey> base = pts.getPointsToSet(pk);
		//					ParameterField field = ParameterFieldFactory.getFactory().getLockField();
		//
		//					HeapWrite write = HeapAccess.createWrite((SyncNode) node, base, field);
		//					hwrites.add(write);
		//				}
		//			}
		//
		//			if (DO_SYNC_METHODS) {
		//				final IMethod im = pdg.getMethod();
		//				if (im.isSynchronized()) {
		//					if (!im.isStatic()) {
		//						int valNum = pdg.getIR().getParameter(THIS_PARAM_NUM);
		//						PointerKey pk = pdg.getPointerKey(valNum);
		//						OrdinalSet<InstanceKey> base = pts.getPointsToSet(pk);
		//						ParameterField field = ParameterFieldFactory.getFactory().getLockField();
		//
		//						HeapWrite writeEntry = HeapAccess.createWrite(pdg.getRoot(), base, field);
		//						hwrites.add(writeEntry);
		//						HeapWrite writeExit = HeapAccess.createWrite(pdg.getExit(), base, field);
		//						hwrites.add(writeExit);
		//					} else {
		//						// search for static field class
		//						IClass cls = im.getDeclaringClass();
		//						IField classField = null;
		//						for (IField field : cls.getAllStaticFields()) {
		//							if (TypeReference.JavaLangClass.getName().equals(field.getFieldTypeReference().getName())) {
		//								if (field.getName().toString().startsWith(CLASS_FIELD_NAME_PREFIX)) {
		//									classField = field;
		//									break;
		//								}
		//							}
		//						}
		//
		//						if (classField != null) {
		//							PointerKey pk = pts.getHeapModel().getPointerKeyForStaticField(classField);
		//							OrdinalSet<InstanceKey> base = pts.getPointsToSet(pk);
		//							ParameterField field = ParameterFieldFactory.getFactory().getLockField();
		//
		//							HeapWrite writeEntry = HeapAccess.createWrite(pdg.getRoot(), base, field);
		//							hwrites.add(writeEntry);
		//							HeapWrite writeExit = HeapAccess.createWrite(pdg.getExit(), base, field);
		//							hwrites.add(writeExit);
		//						} else {
		//							// no class field found - so we create an artificial one. this is some sort of HACK as
		//							// the WALA points-to analysis does not know about this field...
		//							OrdinalSet<InstanceKey> base = pts.getArtificialClassFieldPts(cls);
		//							ParameterField field = ParameterFieldFactory.getFactory().getClassLockField();
		//
		//							HeapWrite writeEntry = HeapAccess.createWrite(pdg.getRoot(), base, field);
		//							hwrites.add(writeEntry);
		//							HeapWrite writeExit = HeapAccess.createWrite(pdg.getExit(), base, field);
		//							hwrites.add(writeExit);
		//						}
		//					}
		//				}
		//			}
		//		}

		return hwrites;
	}

	private OrdinalSet<InstanceKey> getAccessDataFromInstruction(SSAFieldAccessInstruction acc, PDG pdg) {
		OrdinalSet<InstanceKey> base;
		if (acc.isStatic()) {
			base = OrdinalSet.empty();
		} else {
			if (acc.getRef() < 0) {
				// skip operations with illegal value numbers
				if (IS_DEBUG) debug.outln("Skipping instruction because reference value number < 0: " + acc);
				return null;
			}
			base = getPointsToSet(pdg, acc.getRef());
		}
		return base;
	}

	private OrdinalSet<InstanceKey> getAccessDataFromInstruction(SSAArrayReferenceInstruction acc, PDG pdg) {
		return getPointsToSet(pdg, acc.getArrayRef());
	}



	static abstract class HeapAccess {
		private final PDGNode expr;
		private final OrdinalSet<InstanceKey> base;
		private final ParameterField field;

		private HeapAccess(PDGNode expr, OrdinalSet<InstanceKey> base,
				ParameterField field) {
			this.expr = expr;
			this.base = base;
			this.field = field;
		}

		public static HeapWrite createWrite(PDGNode expr,
				OrdinalSet<InstanceKey> base, ParameterField field) {
			HeapWrite hw = new HeapWrite(expr, base, field);
			return hw;
		}

		public static HeapRead createRead(PDGNode expr,
				OrdinalSet<InstanceKey> base, ParameterField field) {
			HeapRead hr = new HeapRead(expr, base, field);
			return hr;
		}

		public PDGNode getNode() {
			return expr;
		}

		public boolean isAliasing(HeapAccess acc) {
			boolean emtpyBases = (base == null && acc.base == null) ||
			(base != null && acc.base != null && base.isEmpty() && acc.base.isEmpty());

			return (field == acc.field) &&
			(emtpyBases || (base != null && base.containsAny(acc.base)));
		}

	}

	static final class HeapRead extends HeapAccess {

		private HeapRead(PDGNode expr, OrdinalSet<InstanceKey> base,
				ParameterField field) {
			super(expr, base, field);
		}
	}

	static final class HeapWrite extends HeapAccess {

		private HeapWrite(PDGNode expr, OrdinalSet<InstanceKey> base,
				ParameterField field) {
			super(expr, base, field);
		}
	}


	/**
	 * Annotate each pdg with the id of thread it may run in. For each entry
	 * to thread.start() two ids are created to model the possibility that
	 * this call may be reached more than once. It doesn't matter how often
	 * it will be called. The only information we are interested in is if it may
	 * be called more than once (>1 instances at the same time) and therefore
	 * the thread may be able to interfere with itself. This is approximated
	 * by adding two ids, everytime we are unsure if multiple instances of the
	 * same thread may exist.
	 * @throws CancelException
	 */
	private final void computeThreadIds(IProgressMonitor progress) throws CancelException {
		if (IS_DEBUG) debug.outln("Computing thread ids");

		IMethod threadStart = findThreadStart();

		Set<CGNode> entryPoints = getListOfAllThreadEntryPoints();

		// start enumeration of threads at 1 - main threads gets special number 0
		int currentThreadId = 1;
		for (CGNode threadRun : entryPoints) {
			PDG pdg = builder.getPDGforMethod(threadRun);
			IntSet transitiveCalled = getTransitiveCallsFromSameThread(threadRun, threadStart);

			final IntSet threadId;
			if (pdg.getMethod().equals(builder.getMainPDG().getMethod())) {
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
					PDG pdg = builder.getPDGforId(x);
					for (IntIterator it = threadId.intIterator(); it.hasNext();) {
						addThreadId(pdg, it.next());
					}
				}

			});

			if (progress.isCanceled()) {
				throw CancelException.make("Copmuting thread ids canceled.");
			}

			progress.worked(1);
		}
	}

	private void addThreadId(PDG pdg, int threadId) {
		MutableIntSet thrIds;
		if (!threadIdsOfPDGs.containsKey(pdg)) {
			thrIds = new BitVectorIntSet();
			threadIdsOfPDGs.put(pdg, thrIds);
		} else {
			thrIds = threadIdsOfPDGs.get(pdg);
		}

		thrIds.add(threadId);
	}

	private final Set<CGNode> getListOfAllThreadEntryPoints() {
		return tiProvider.getAllThreadEntryNodesInCallGraph();
	}

	private final IMethod findThreadStart() {
		return tiProvider.getThreadStartMethod();
	}

	private final MutableIntSet getClinitsIntSet() {
		MutableIntSet clinitIds = new BitVectorIntSet();
		if (!ignoreClInits) {
			for (PDG pdg : builder.getAllPDGs()) {
				if (pdg.getMethod().isClinit()) {
					clinitIds.add(pdg.getId());
				}
			}
		}
		return clinitIds;
	}

	/**
	 * Computes a set of all pdgs whose method may be called subsequently by the method
	 * provided as {@code threadRun}. It stops at any call of {@code Thread.start()}, since the current
	 * thread is left at these call sites.<br>
	 * The result is stored in an int set where each entry is
	 * the pdg-id of a pdg that may be called.
	 * @param threadRun the call sites at which the search starts
	 * @param threadStart represents the {@code Thread.start()} method of the program to be analyzed
	 * @return intset of pdg ids
	 */
	private final IntSet getTransitiveCallsFromSameThread(CGNode threadRun, final IMethod threadStart) {
		MutableIntSet called = new BitVectorIntSet();

		PDG pdg = builder.getPDGforMethod(threadRun);
		called.add(pdg.getId());

		searchCalleesInSameThread(threadRun, called, threadStart);

		return called;
	}

	private final void searchCalleesInSameThread(CGNode caller, MutableIntSet called, final IMethod threadStart) {
		for (Iterator<CGNode> it = getCallGraph().getSuccNodes(caller); it.hasNext();) {
			CGNode node = it.next();
			PDG pdgCur = builder.getPDGforMethod(node);
			if (pdgCur != null && !called.contains(pdgCur.getId())) {
				called.add(pdgCur.getId());
				// only look further if no new thread has been created
				if (!pdgCur.getMethod().equals(threadStart)) {
					searchCalleesInSameThread(node, called, threadStart);
				}
			}
		}
	}

}
