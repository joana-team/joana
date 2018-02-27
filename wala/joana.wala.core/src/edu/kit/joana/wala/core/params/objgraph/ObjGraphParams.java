/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph;

import static edu.kit.joana.wala.util.pointsto.WalaPointsToUtil.unify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.graph.traverse.DFSFinishTimeIterator;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.util.Config;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.ParameterFieldFactory;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.dataflow.GenReach;
import edu.kit.joana.wala.core.joana.DumpSDG;
import edu.kit.joana.wala.core.params.objgraph.ModRefCandidates.InterProcCandidateModel;
import edu.kit.joana.wala.core.params.objgraph.TVL.V;
import edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory;
import edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactoryImpl;
import edu.kit.joana.wala.core.params.objgraph.candidates.MergeByPartition;
import edu.kit.joana.wala.core.params.objgraph.candidates.MergeStrategy;
import edu.kit.joana.wala.core.params.objgraph.dataflow.ModRefDataFlow;
import edu.kit.joana.wala.core.params.objgraph.dataflow.PointsToWrapper;
import edu.kit.joana.wala.util.PrettyWalaNames;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public final class ObjGraphParams {

	public static void compute(final SDGBuilder sdg, final Options opt, final IProgressMonitor progress)
			throws CancelException {
		final ObjGraphParams objparams = new ObjGraphParams(sdg, opt);
		objparams.run(progress);
	}

	private final SDGBuilder sdg;
	private final Options opt;

	public static final class Options {

		public Options() {
			this.isCutOffUnreachable = false;
			this.isMergeException = true;
			this.isCutOffImmutables = true;
			this.isMergeOneFieldPerParent = true;
			this.isMergePrunedCallNodes = true;
			this.isMergeDuringCutoffImmutables = true;
			this.isUseAdvancedInterprocPropagation = true;
			this.maxNodesPerInterface = DEFAULT_MAX_NODES_PER_INTERFACE;
			this.convertToObjTree = false;
			this.doStaticFields = false;
			this.ignoreExceptions = false;
		}

		private void adjustWithProperties() {
			if (Config.isDefined(Config.C_OBJGRAPH_ADVANCED_INTERPROC_PROP)) {
				isUseAdvancedInterprocPropagation = Config.getBool(Config.C_OBJGRAPH_ADVANCED_INTERPROC_PROP);
			}
			
			if (Config.isDefined(Config.C_OBJGRAPH_CONVERT_TO_OBJTREE)) {
				convertToObjTree = Config.getBool(Config.C_OBJGRAPH_CONVERT_TO_OBJTREE);
			}
			
			if (Config.isDefined(Config.C_OBJGRAPH_CUT_OFF_IMMUTABLE)) {
				isCutOffImmutables = Config.getBool(Config.C_OBJGRAPH_CUT_OFF_IMMUTABLE);
			}
			
			if (Config.isDefined(Config.C_OBJGRAPH_CUT_OFF_UNREACHABLE)) {
				isCutOffUnreachable = Config.getBool(Config.C_OBJGRAPH_CUT_OFF_UNREACHABLE);
			}
			
			if (Config.isDefined(Config.C_OBJGRAPH_MAX_NODES_PER_INTERFACE)) {
				maxNodesPerInterface =
					Config.getInt(Config.C_OBJGRAPH_MAX_NODES_PER_INTERFACE, Options.DEFAULT_MAX_NODES_PER_INTERFACE);
			}
			
			if (Config.isDefined(Config.C_OBJGRAPH_MERGE_EXCEPTIONS)) {
				isMergeException = Config.getBool(Config.C_OBJGRAPH_MERGE_EXCEPTIONS);
			}
			
			if (Config.isDefined(Config.C_OBJGRAPH_MERGE_ONE_FIELD_PER_PARENT)) {
				isMergeOneFieldPerParent = Config.getBool(Config.C_OBJGRAPH_MERGE_ONE_FIELD_PER_PARENT);
			}
			
			if (Config.isDefined(Config.C_OBJGRAPH_MERGE_PRUNED_CALL_NODES)) {
				isMergePrunedCallNodes = Config.getBool(Config.C_OBJGRAPH_MERGE_PRUNED_CALL_NODES);
			}

			if (Config.isDefined(Config.C_OBJGRAPH_DO_STATIC_FIELDS)) {
				doStaticFields = Config.getBool(Config.C_OBJGRAPH_DO_STATIC_FIELDS);
			}
		}
		
		/**
		 * Convert the result to an object tree by duplicating nodes with multiple predecessors
		 */
		public boolean convertToObjTree;

		/**
		 * Remove all side effects that are not reachable through root nodes (parameter or static fields).
		 * This is basically an escape analysis that detects and removes invisible side-effects.
		 */
		public boolean isCutOffUnreachable;

		/**
		 * Cut off fields that are only reachable through immutable types. They cannot be changed.
		 * Merge fields for the constructor, cut off fields everywhere else.
		 */
		public boolean isCutOffImmutables;

		public static final Set<TypeReference> IMMUTABLES;
		static {
			IMMUTABLES = new HashSet<TypeReference>();
			IMMUTABLES.add(TypeReference.JavaLangString);
			IMMUTABLES.add(TypeReference.JavaLangLong);
			IMMUTABLES.add(TypeReference.JavaLangCharacter);
			IMMUTABLES.add(TypeReference.JavaLangInteger);
			IMMUTABLES.add(TypeReference.JavaLangDouble);
			IMMUTABLES.add(TypeReference.JavaLangBoolean);
			IMMUTABLES.add(TypeReference.JavaLangByte);
			IMMUTABLES.add(TypeReference.JavaLangFloat);
			IMMUTABLES.add(TypeReference.JavaLangShort);
		};

		/**
		 * Compute also nodes and data deps for static fields - normally handled by a separate previous phase.
		 */
		public boolean doStaticFields;

		/**
		 * Do not propagate side-effects that occur due to exceptions.
		 */
		public boolean ignoreExceptions;

		/**
		 * Merge all fields that are only reachable through an exception to a single node.
		 */
		public boolean isMergeException;

		/**
		 * Merge field nodes that share a common parent and refer to the same field. This results in the object graph
		 * structure as described in the SCAM2010 paper.
		 */
		public boolean isMergeOneFieldPerParent;

		/**
		 * Merge all fields that are only referenced or modified through methods that were pruned in the cg.
		 */
		public boolean isMergePrunedCallNodes;

		/**
		 * Merge fields of immutables during cutoff
		 */
		public boolean isMergeDuringCutoffImmutables;

		
		/**
		 * Use advanced (and slower fixed point based) interprocedural propagation with reachability check during propagation.
		 */
		public boolean isUseAdvancedInterprocPropagation;

		/**
		 * Maximal number of nodes for a single method interface (formal-nodes). If more nodes are part
		 * of the interface, they will be merged.
		 */
		public int maxNodesPerInterface;

		public static final int UNLIMITED_NODES_PER_INTERFACE = -1;
		public static final int DEFAULT_MAX_NODES_PER_INTERFACE = 40;
		/**
		 * If maxNodePerInterface is set, this constant defines at which depth level in the graph structure
		 * the merging of nodes should start. So iff #nodes > maxNodesPerInterface we try to merge all nodes
		 * reachable from the same node n at depth STD_MERGE_LEVEL. Depth means the minimal distance from any
		 * root node. Iff the merge operation does not result in #nodes <= maxNodesPerInterface, the operation
		 * continues at the next smaller depth level until maxNodesPerInterface is reached or depth == 0.
		 *
		 * Experiments have shown that 3 is a good starting point as there are not many nodes at depth > 4 in general.
		 * This is partly due to structure of most code and partly due to points to imprecision. One perhaps may want
		 * to increase this iff the points-to precision has been significantly improved as well as maxNodesPerInterface
		 * is set to a larger threshold (ca. >50). Otherwise increasing this value will have no effect, as the depth
		 * level will be decreased until the threshold is reached.
		 * 
		 * As we optimized our depth merge computation, we can start at 5 without loosing too much speed, because we
		 * automatically merge at a lower level as soon as we notice that the number of individual nodes will be too
		 * huge otherwise.
		 */
		public static final int STD_MERGE_LEVEL = 5;

	}

	public static final int FLAG_MERGE_EXCEPTION 		= 1 << 0;
	public static final int FLAG_MERGE_IMMUTABLE 		= 1 << 1;
	public static final int FLAG_MERGE_SAME_FIELD 		= 1 << 2;
	public static final int FLAG_MERGE_PRUNED_CALL 		= 1 << 3;
	public static final int FLAG_MERGE_AT_DEPTH 		= 1 << 4;
	public static final int FLAG_MERGE_STATICS			= 1 << 5;
	public static final int FLAG_MERGE_SMUSH_MANY 		= 1 << 6;
	public static final int FLAG_MERGE_INITIAL_LIBRARY 	= 1 << 7;
	
	private ObjGraphParams(final SDGBuilder sdg, final Options opt) {
		this.sdg = sdg;
		this.opt = opt;
		this.opt.adjustWithProperties();
	}
	
	private void run(final IProgressMonitor progress) throws CancelException {
		final Logger logStats = Log.getLogger(Log.L_OBJGRAPH_STATS);
		sdg.cfg.out.print("(if");

		// step 1 create candidates
		final CallGraph cg = sdg.getNonPrunedWalaCallGraph();
		final ParameterFieldFactory pfact = sdg.getParameterFieldFactory();
		final OrdinalSetMapping<ParameterField> fieldMapping = pfact.getMapping();

		final CandidateFactory candFact;
		if (sdg.cfg.mergeFieldsOfPrunedCalls) {
			final LinkedList<Set<ParameterField>> partition = sdg.partitions;
			assert partition != null;
			sdg.partitions = null;	// after this step we don't need them anymore
			final MergeByPartition mergeAll = new MergeByPartition(partition);
			candFact = new CandidateFactoryImpl(mergeAll, fieldMapping);
			mergeAll.setFactory(candFact);
		} else {
			candFact = new CandidateFactoryImpl(MergeStrategy.NO_INITIAL_MERGE, fieldMapping);
		}

        int progressCtr = 0;
        if (progress != null) {
            int totalSteps = 4;
            if ((sdg.cfg.sideEffects != null) || SideEffectDetectorConfig.isActivated()) totalSteps+=2;
            if (sdg.cfg.localKillingDefs) totalSteps++;
            if (opt.convertToObjTree) totalSteps++;
            
            progress.beginTask("interproc: adding data flow for heap fields...", totalSteps);
            progress.subTask("step 1: create candidates");
        }

		final ModRefCandidates mrefs = ModRefCandidates.computeIntracProc(pfact, candFact, cg,
				sdg.getPointerAnalysis(), opt.doStaticFields, opt.ignoreExceptions, progress, sdg.isParallel());

		// create side-effect detector if command line option has been set.
		if (sdg.cfg.sideEffects == null && SideEffectDetectorConfig.isActivated()) {
			sdg.cfg.sideEffects = SideEffectDetectorConfig.maybeCreateInstance();
		}
		
		if (sdg.cfg.sideEffects != null) {
			sdg.cfg.sideEffects.copyIntraprocState(mrefs);
            if (progress != null) progress.worked(++progressCtr);
		}
		
        // step 2 propagate interprocedural
		long t1 = 0, t2 = 0;
		if (logStats.isEnabled()) { t1 = System.currentTimeMillis(); }

		final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> interModRef;
		if (opt.isUseAdvancedInterprocPropagation) {
            if (progress != null) progress.subTask("step 2: propagate interprocedural - fixpointReachabilityPropagate");
			interModRef = fixpointReachabilityPropagate(sdg, cg, mrefs, progress);
		} else {
            if (progress != null) progress.subTask("step 2: propagate interprocedural - simpleReachabilityPropagate");
			interModRef = simpleReachabilityPropagate(cg, sdg.getWalaCallGraph(), mrefs,
					new PointsToWrapper(sdg.getPointerAnalysis()), progress);
		}
	    if (progress != null) progress.worked(++progressCtr);
		
        long sdgNodeCount = 0;
		if (logStats.isEnabled()) {
			t2 = System.currentTimeMillis();
			sdgNodeCount = sdg.countNodes();
		}
        if (progress != null) progress.subTask("step 2 propagate interprocedural - adjustModRef");

		sdg.cfg.out.print(",adj");

		adjustInterprocModRef(cg, interModRef, mrefs, sdg, progress);
        if (progress != null) progress.worked(++progressCtr);
		
        if (sdg.cfg.sideEffects != null) {
			// detect modifications to a given pointerkey
			sdg.cfg.out.print(",se");
            if (progress != null) progress.subTask("side effects");
			sdg.cfg.sideEffects.runAnalysis(sdg, cg, mrefs, progress);
			// free memory
			sdg.cfg.sideEffects = null;
            if (progress != null) progress.worked(++progressCtr);
		}
		
		sdg.cfg.out.print(",df");
        if (progress != null) progress.subTask("dataflow");
		ModRefDataFlow.compute(mrefs, sdg, progress);
	    if (progress != null) progress.worked(++progressCtr);

	    if (opt.isCutOffImmutables) {
	    	// connect initializer out nodes with this-pointer of immutable object. 
	        if (progress != null) progress.subTask("immutable optimization");
	        connectImmutableInitializersToThisPtr(mrefs, sdg, progress);
		    if (progress != null) progress.worked(++progressCtr);
	    }
	    
        if (sdg.cfg.localKillingDefs) {
			sdg.registerFinalModRef(mrefs, progress);
			sdg.cfg.out.print(",reg");
            if (progress != null) progress.worked(++progressCtr);
		}

		long sdgNodeCountGraph = 0;
		if (opt.convertToObjTree) {
			DumpSDG.dumpIfEnabled(sdg, Log.L_SDG_DUMP_PRE_TREE);
			sdg.cfg.out.print(",2tree");
            if (progress != null) progress.subTask("converting to object tree");

			if (logStats.isEnabled()) {
				sdgNodeCountGraph = sdg.countNodes();
			}

			ObjTreeConverter.convert(sdg, progress);
            if (progress != null) progress.worked(++progressCtr);
		}

		sdg.cfg.out.print(")");
        if (progress != null) progress.done();

		if (logStats.isEnabled()) {
			long t3 = System.currentTimeMillis();
			long cands = mrefs.countCandidates();
			long sdgNodeCountTotal = sdg.countNodes();
			final long paramsTotal = sdgNodeCountTotal - sdgNodeCount;

			logStats.outln("\n---- BEGIN: Parameter Propagation Statistics ----\n");
			logStats.outln("propagation time    : " + (t2 - t1) + " ms");
			logStats.outln("optimization time   : " + (t3 - t2) + " ms");
			logStats.outln("total time          : " + (t3 - t1) + " ms");
			logStats.outln("number of candidates: " + cands);
			if (opt.convertToObjTree) {
				final long paramNodesGraphs = sdgNodeCountGraph - sdgNodeCount;
				final long paramNodesTree = sdgNodeCountTotal - sdgNodeCountGraph;

				logStats.outln("obj-graph new nodes : " + paramNodesGraphs + " ("
						+ ((100 * paramNodesGraphs) / sdgNodeCountTotal) + "% total)");
				logStats.outln("graph2tree new nodes: " + (sdgNodeCountTotal - sdgNodeCountGraph)
						+ " (" + ((100 * paramNodesTree) / sdgNodeCountTotal) + "% total) ("
						+ ((100 * paramNodesTree) / paramsTotal) + "% params)");
			}
			logStats.outln("number of new nodes : " + paramsTotal + " ("
					+ ((100 * paramsTotal) / sdgNodeCountTotal) + "%)");
			logStats.outln("tot. number of nodes: " + sdgNodeCountTotal);
			logStats.outln("\n---- END: Parameter Propagation Statistics ----\n");
		}
	}
	
	private static void connectImmutableInitializersToThisPtr(final ModRefCandidates mrefs, final SDGBuilder sdg,
			final IProgressMonitor progress) {
		for (final PDG pdg: sdg.getAllPDGs()) {
			for (final PDGNode n : pdg.getCalls()) {
				final SSAInstruction ii = pdg.getInstruction(n);
				if (ii instanceof SSAInvokeInstruction) {
					final SSAInvokeInstruction invk = (SSAInvokeInstruction) ii;
					final MethodReference mref = invk.getDeclaredTarget();
					if (mref.isInit() && sdg.isImmutableStub(mref.getDeclaringClass())) {
						// found a call to an init method that needs to be handled.
						final PDGNode newStmt = searchMatchingNewStatement(pdg, n);
						final PDGNode imm = pdg.createDummyNode("immutable");
						PDGEdge toRemove = null;
						PDGNode succ = null;
						for (final PDGEdge e : pdg.outgoingEdgesOf(n)) {
							if (e.kind == PDGEdge.Kind.CONTROL_DEP_EXPR && e.to.getKind() == PDGNode.Kind.ACTUAL_OUT) {
								pdg.addEdge(e.to, imm, PDGEdge.Kind.DATA_DEP);
							} else if (e.kind == PDGEdge.Kind.CONTROL_FLOW) {
								toRemove = e;
								succ = e.to;
							}
						}
						pdg.addEdge(n, imm, PDGEdge.Kind.CONTROL_DEP_EXPR);
						pdg.addEdge(imm, newStmt, PDGEdge.Kind.DATA_DEP);
						
						pdg.removeEdge(toRemove);
						pdg.addEdge(n, imm, PDGEdge.Kind.CONTROL_FLOW);
						pdg.addEdge(imm, succ, PDGEdge.Kind.CONTROL_FLOW);
					}
				}
			}
		}
	}

	private static PDGNode searchMatchingNewStatement(final PDG pdg, final PDGNode call) {
		final PDGNode[] ins = pdg.getParamIn(call);
		if (ins == null || ins.length == 0) {
			return null;
		}
		
		final PDGNode thisPtr = ins[0];
		assert (thisPtr.getBytecodeIndex() == BytecodeLocation.ROOT_PARAMETER 
				&& thisPtr.getBytecodeName().equals(BytecodeLocation.getRootParamName(0)))
			: "expected this pointer, found " + thisPtr;
		
		// this-pointer of initializers are directly connected to the appropriate new statement through a dd edge
		PDGNode newStmt = null;
		for (final PDGEdge e : pdg.incomingEdgesOf(thisPtr)) {
			if (e.kind == PDGEdge.Kind.DATA_DEP) {
				newStmt = e.from;
				break;
			}
		}
		
		return newStmt;
	}
	
	private void adjustInterprocModRef(final CallGraph cg, final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> interModRef,
			final ModRefCandidates modref, final SDGBuilder sdg, final IProgressMonitor progress) throws CancelException {
		final Logger debug = Log.getLogger(Log.L_OBJGRAPH_DEBUG);
		final boolean isDebug = debug.isEnabled();
        //int progressCtr = 0;

		// add all nodes to the interface
		for (final CGNode n : cg) {
			MonitorUtil.throwExceptionIfCanceled(progress);
			final OrdinalSet<ModRefFieldCandidate> pimod = interModRef.get(n);

			if (pimod != null && !pimod.isEmpty()) {
				final InterProcCandidateModel interCands = modref.getCandidates(n);

				for (final ModRefFieldCandidate c : pimod) {
					interCands.addCandidate(c);
				}
			}
		}
		
		// merge nodes for cut off cgnodes
		if (opt.isMergePrunedCallNodes) {
			if (isDebug) { debug.out("merge pruned call nodes... "); }
			mergePrunedCallNodes(sdg, cg, interModRef, modref, progress);
			if (isDebug) { debug.outln("done."); }
		}

		if (opt.isCutOffUnreachable || opt.isMergeException || opt.isCutOffImmutables || opt.isMergeOneFieldPerParent
				|| opt.maxNodesPerInterface != Options.UNLIMITED_NODES_PER_INTERFACE) {
			final PointsToWrapper pa = new PointsToWrapper(sdg.getPointerAnalysis());

            if (progress != null) {
                progress.beginTask("adjustInterprocModRef", sdg.getAllPDGs().size());
            }
			Stream<PDG> s = sdg.isParallel()?sdg.getAllPDGs().parallelStream():sdg.getAllPDGs().stream();
			s.forEach(pdg -> {
				//MonitorUtil.throwExceptionIfCanceled(progress);
				final InterProcCandidateModel pdgModRef = modref.getCandidates(pdg.cgNode);

				if (pdgModRef == null) { /*progressCtr++;*/ return; }

                /*if (progress != null) {
                    progress.subTask(pdg.getMethod().toString());
                    progress.worked(progressCtr++);
                }*/

				final ModRefCandidateGraph mrg = ModRefCandidateGraph.compute(pa, modref, pdg);

				final int initialNodeCount = pdgModRef.size();
				int lastNodeCount = initialNodeCount; 
				if (isDebug) { debug.out(PrettyWalaNames.methodName(pdg.getMethod()) +" (" + lastNodeCount + "):");	}


				// cut off unreachable nodes
				if (opt.isCutOffUnreachable) {
					if (isDebug) { debug.out(" a[1"); }
					cutOffUnreachable(pdgModRef, mrg, mrg.getRoots());
					if (isDebug) {
						final int curNodeCount = pdgModRef.size();
						debug.out("](-" + (lastNodeCount - curNodeCount) + ")");
						lastNodeCount = curNodeCount;
					}
				}

				// merge nodes only reachable from exceptions
				if (opt.isMergeException) {
					if (isDebug) { debug.out(" a[2"); }
					mergeException(pdgModRef, mrg);
					if (isDebug) {
						final int curNodeCount = pdgModRef.size();
						debug.out("](-" + (lastNodeCount - curNodeCount) + ")");
						lastNodeCount = curNodeCount;
					}
				}

				// cut off fields of immutables
				if (opt.isCutOffImmutables) {
					if (isDebug) { debug.out(" a[3"); }
					cutOffImmutables(pdgModRef, mrg, pdg);
					if (isDebug) {
						final int curNodeCount = pdgModRef.size();
						debug.out("](-" + (lastNodeCount - curNodeCount) + ")");
						lastNodeCount = curNodeCount;
					}
				}

				if (opt.isMergeOneFieldPerParent) {
					if (isDebug) { debug.out(" a[4"); }
					mergeOneFieldPerParent(pdgModRef, mrg);
					if (isDebug) {
						final int curNodeCount = pdgModRef.size();
						debug.out("](-" + (lastNodeCount - curNodeCount) + ")");
						lastNodeCount = curNodeCount;
					}
				}

				if (opt.maxNodesPerInterface != Options.UNLIMITED_NODES_PER_INTERFACE
						&& opt.maxNodesPerInterface < pdgModRef.size()) {

					if (pdgModRef.size() > opt.maxNodesPerInterface) {
						if (isDebug) { debug.out(" a[5"); }
						// if the interface is still too big, we try to merge all locations only reachable through
						// static fields to a single candidate.
						mergeAllStatics(pdgModRef, mrg);
						if (isDebug) {
							final int curNodeCount = pdgModRef.size();
							debug.out("](-" + (lastNodeCount - curNodeCount) + ")");
							lastNodeCount = curNodeCount;
						}
					}

					if (pdgModRef.size() > opt.maxNodesPerInterface) {
						if (isDebug) { debug.out(" a[6"); }
						final int threshold = opt.maxNodesPerInterface / 2;
						smushManySameFields(pdgModRef, threshold, progress);
						if (isDebug) {
							final int curNodeCount = pdgModRef.size();
							debug.out("](-" + (lastNodeCount - curNodeCount) + ")");
							lastNodeCount = curNodeCount;
						}
					}
					
					if (pdgModRef.size() > opt.maxNodesPerInterface) {
						if (isDebug) { debug.out(" a[7"); }
						mergeAutomaticAtDepthLevel(pdgModRef, mrg, opt.maxNodesPerInterface, debug);
						if (isDebug) {
							final int curNodeCount = pdgModRef.size();
							debug.out("](-" + (lastNodeCount - curNodeCount) + ")");
							lastNodeCount = curNodeCount;
						}
					}

				}
				
				if (isDebug) {
					lastNodeCount = pdgModRef.size();
					debug.outln(" ==>(" + lastNodeCount + " "
							+ (initialNodeCount > 0 ? ((100 *lastNodeCount) / initialNodeCount) : "--") + "%)");
				}
			});
		}

		if (isDebug) { debug.outln(""); }

	}

	private static void smushManySameFields(final InterProcCandidateModel pdgModRef, final int threshold,
			final IProgressMonitor progress) {
		final TObjectIntMap<ParameterField> counter = new TObjectIntHashMap<ParameterField>();
		for (final ModRefFieldCandidate mrf : pdgModRef) {
			if (!mrf.pc.isUnique()) { continue; }
			
			final OrdinalSet<ParameterField> fields = mrf.getFields();
			if (fields.size() == 1) {
				final ParameterField pf = fields.iterator().next();
				int count = (counter.containsKey(pf) ? counter.get(pf) : 0);
				count++;
				counter.put(pf, count);
			}
		}
		
		for (final TObjectIntIterator<ParameterField> it = counter.iterator(); it.hasNext();) {
			it.advance();
			final int count = it.value();
			if (count > threshold) {
				final ParameterField field = it.key();
				mergeAllSingleFields(pdgModRef, field);
			}
		}

        if (progress != null) { progress.done(); }
	}

	private static void mergeAllSingleFields(final InterProcCandidateModel pdgModRef, final ParameterField field) {
		final List<ModRefFieldCandidate> toMerge = new LinkedList<ModRefFieldCandidate>();
		for (final ModRefFieldCandidate mrf : pdgModRef) {
			if (!mrf.pc.isUnique()) { continue; }

			final OrdinalSet<ParameterField> fields = mrf.getFields();
			if (fields.size() == 1) {
				final ParameterField pf = fields.iterator().next();
				if (pf.equals(field)) {
					toMerge.add(mrf);
				}
			}
		}
		if (!toMerge.isEmpty()) {
			pdgModRef.mergeCandidates(toMerge, FLAG_MERGE_SMUSH_MANY);
		}
	}
	
	private static void cutOffUnreachable(final InterProcCandidateModel pdgModRef, final ModRefCandidateGraph mrg,
			final List<? extends ModRefCandidate> start) {
		final Set<ModRefCandidate> reachable = findReachable(mrg, start);
		final List<ModRefFieldCandidate> toRemove = new LinkedList<ModRefFieldCandidate>();
		for (final ModRefFieldCandidate c : pdgModRef) {
			if (!reachable.contains(c)) {
				toRemove.add(c);
			}
		}

		for (final ModRefFieldCandidate c : toRemove) {
			pdgModRef.removeCandidate(c);
		}
	}

	private static void mergeException(final InterProcCandidateModel pdgModRef, final ModRefCandidateGraph mrg) {
		final List<ModRefRootCandidate> exc = new LinkedList<ModRefRootCandidate>();
		final List<ModRefRootCandidate> normal = new LinkedList<ModRefRootCandidate>();
		for (final ModRefRootCandidate r : mrg.getRoots()) {
			if (r.isException()) {
				exc.add(r);
			} else {
				normal.add(r);
			}
		}

		final Set<ModRefCandidate> reachExc = findReachable(mrg, exc);
		final Set<ModRefCandidate> reachNorm = findReachable(mrg, normal);
		reachExc.removeAll(reachNorm);

		final List<ModRefFieldCandidate> toMerge = new LinkedList<ModRefFieldCandidate>();
		for (final ModRefCandidate c : reachExc) {
			if (c instanceof ModRefFieldCandidate) {
				toMerge.add((ModRefFieldCandidate) c);
			}
		}

		if (!toMerge.isEmpty()) {
			pdgModRef.mergeCandidates(toMerge, FLAG_MERGE_EXCEPTION);
		}
	}

	private void cutOffImmutables(final InterProcCandidateModel pdgModRef, final ModRefCandidateGraph mrg,
			final PDG pdg) {
		final TypeReference mergeType;

		if (pdg.getMethod().isInit() && Options.IMMUTABLES.contains(pdg.getMethod().getDeclaringClass().getReference())) {
			// this method is a constructor for an immutable class. We do not cut off the fields for this
			// type, we merge them here. So the side-effects in the constructor (the only place for side-effects
			// of immutable classes) is captured.
			mergeType = pdg.getMethod().getDeclaringClass().getReference();
		} else {
			mergeType = null;
		}

		final List<ModRefCandidate> cutOff = new LinkedList<ModRefCandidate>();
		final List<ModRefCandidate> merge = new LinkedList<ModRefCandidate>();

		for (final ModRefRootCandidate r : mrg.getRoots()) {
			if (Options.IMMUTABLES.contains(r.getType())) {
				if (mergeType != null && mergeType == r.getType()) {
					merge.add(r);
				} else {
					cutOff.add(r);
				}
			}
		}

		for (final ModRefFieldCandidate c : pdgModRef) {
			if (Options.IMMUTABLES.contains(c.getType())) {
				if (mergeType != null && mergeType == c.getType()) {
					merge.add(c);
				} else {
					cutOff.add(c);
				}
			}
		}

		if (!cutOff.isEmpty() || !merge.isEmpty()) {
			final Set<ModRefCandidate> reachCut =
				(cutOff.isEmpty() ? new HashSet<ModRefCandidate>() : findReachable(mrg, cutOff));
			final Set<ModRefCandidate> reachMerge =
				(merge.isEmpty() ? new HashSet<ModRefCandidate>() : findReachable(mrg, merge));
			reachCut.removeAll(reachMerge);
			reachCut.removeAll(cutOff);
			reachMerge.removeAll(merge);

			for (final ModRefCandidate c : reachCut) {
				if (c instanceof ModRefFieldCandidate) {
					pdgModRef.removeCandidate((ModRefFieldCandidate) c);
				}
			}

			if (!reachMerge.isEmpty()) {
				final List<ModRefFieldCandidate> toMerge = new LinkedList<ModRefFieldCandidate>();
				for (final ModRefCandidate c : reachMerge) {
					if (c instanceof ModRefFieldCandidate) {
						toMerge.add((ModRefFieldCandidate) c);
					}
				}
				if (this.opt.isMergeDuringCutoffImmutables) {
					pdgModRef.mergeCandidates(toMerge, FLAG_MERGE_IMMUTABLE);
				}
			}
		}

		// cut of fields that may be unreachable now.
		if (!cutOff.isEmpty() && opt.isCutOffUnreachable) {
			cutOffUnreachable(pdgModRef, mrg, mrg.getRoots());
		}
	}

	private static void mergeOneFieldPerParent(final InterProcCandidateModel pdgModRef, final ModRefCandidateGraph mrg) {
		final LinkedList<ModRefCandidate> work = new LinkedList<ModRefCandidate>();
		work.addAll(mrg.getRoots());
		final Set<ModRefCandidate> visited = new HashSet<ModRefCandidate>();
		visited.addAll(work);

		while (!work.isEmpty()) {
			final ModRefCandidate c = work.removeFirst();

			if (!mrg.containsNode(c)) {
				// may be obsolete, because it has been merged
				continue;
			}

			mergeSameFieldsOfParent(pdgModRef, mrg, c);

			final Iterator<ModRefCandidate> it = mrg.getSuccNodes(c);
			while (it.hasNext()) {
				final ModRefCandidate toCheck = it.next();
				if (!visited.contains(toCheck)) {
					work.add(toCheck);
					visited.add(toCheck);
				}
			}
		}
	}

	private static void mergeSameFieldsOfParent(final InterProcCandidateModel pdgModRef, final ModRefCandidateGraph mrg,
			final ModRefCandidate parent) {
		final Map<ParameterField, Set<ModRefFieldCandidate>> field2cand =
			new HashMap<ParameterField, Set<ModRefFieldCandidate>>();

		final Iterator<ModRefCandidate> it = mrg.getSuccNodes(parent);
		while (it.hasNext()) {
			final ModRefFieldCandidate succ = (ModRefFieldCandidate) it.next();
			assert pdgModRef.contains(succ);

			final OrdinalSet<ParameterField> fields = succ.getFields();
			
			assert fields.size() > 0;
			if (fields.size() > 1) continue;
			
			for (final ParameterField f : fields) { 
				Set<ModRefFieldCandidate> fcs = field2cand.get(f);
				if (fcs == null) {
					fcs = new HashSet<ModRefFieldCandidate>();
					field2cand.put(f, fcs);
				}

				fcs.add(succ);
			}
		}

		for (final Entry<ParameterField, Set<ModRefFieldCandidate>> entry : field2cand.entrySet()) {
			final Set<ModRefFieldCandidate> cands = entry.getValue();

			if (cands.size() > 1) {
				pdgModRef.mergeCandidates(cands, FLAG_MERGE_SAME_FIELD);
			}
		}
	}

	private static void mergePrunedCallNodes(final SDGBuilder sdg, final CallGraph cg,
			final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> interModRef, final ModRefCandidates modref,
			final IProgressMonitor progress) throws CancelException {
		final CallGraph pruned = sdg.getWalaCallGraph();
		final Set<CGNode> prunedCGs = new HashSet<CGNode>();

		for (final PDG pdg : sdg.getAllPDGs()) {
			final Iterator<CGNode> succIt = cg.getSuccNodes(pdg.cgNode);
			while (succIt.hasNext()) {
				final CGNode succ = succIt.next();
				if (!pruned.containsNode(succ)) {
					prunedCGs.add(succ);
				}
			}
		}

		for (final CGNode n : prunedCGs) {
			MonitorUtil.throwExceptionIfCanceled(progress);
			final OrdinalSet<ModRefFieldCandidate> pimod = interModRef.get(n);

			if (!pimod.isEmpty()) {
				final InterProcCandidateModel interCands = modref.getCandidates(n);
				interCands.mergeCandidates(pimod, FLAG_MERGE_PRUNED_CALL);
			}
		}
	}

	private static void mergeAutomaticAtDepthLevel(final InterProcCandidateModel pdgModRef, final ModRefCandidateGraph mrg,
			final int maxNodes, final Logger debug) {
		final int maxDepthLevel = Options.STD_MERGE_LEVEL;

		OrdinalSet<InstanceKey> reachPts = mrg.getRootsPts();

		final Set<ModRefFieldCandidate> fields = new HashSet<ModRefFieldCandidate>();
		for (final ModRefFieldCandidate fc : pdgModRef) {
			// do not merge fields previously merged through merge statics. Those are merge nodes that consist
			// of a large number of individual field accesses. Further merging them would harm the precision.
			if ((fc.flags & FLAG_MERGE_STATICS) == 0) {
				fields.add(fc);
			}
		}
				
		List<ModRefFieldCandidate> border = new LinkedList<ModRefFieldCandidate>();
		final List<ModRefFieldCandidate> reachable = new LinkedList<ModRefFieldCandidate>();
		
		for (int i = maxDepthLevel; i > 0; i--) {
			final List<ModRefFieldCandidate> notReachable = new LinkedList<ModRefFieldCandidate>();
			final List<ModRefFieldCandidate> reachTmp = new LinkedList<ModRefFieldCandidate>();
			
			for (final ModRefFieldCandidate fc : fields) {
				if (fc.isReachableFrom(reachPts)) {
					reachTmp.add(fc);
				} else {
					notReachable.add(fc);
				}
			}
			
			debug.out(".");
			
			fields.removeAll(reachTmp);
			if (reachable.isEmpty() && reachTmp.size() > maxNodes) {
				reachable.addAll(reachTmp);
				border = reachTmp;
				break;
			} else if (!reachable.isEmpty() && (reachable.size() + reachTmp.size() > maxNodes)) {
				break;
			}
			
			reachable.addAll(reachTmp);
			border = reachTmp;

			if (i > 1) {
				for (final ModRefFieldCandidate fc : reachTmp) {
					reachPts = unify(reachPts, fc.pc.getFieldPointsTo());
				}
			}
			
		}		
		
		for (final ModRefCandidate c : border) {
			// merge reachable
			final List<ModRefCandidate> single = new LinkedList<ModRefCandidate>();
			single.add(c);
			final Set<ModRefCandidate> singleReach = findReachable(mrg, single);

			final List<ModRefFieldCandidate> toMerge = new LinkedList<ModRefFieldCandidate>();
			for (final ModRefCandidate r : singleReach) {
				if (r instanceof ModRefFieldCandidate && !reachable.contains(r)) {
					toMerge.add((ModRefFieldCandidate) r);
				}
			}

			if (!toMerge.isEmpty()) {
				pdgModRef.mergeCandidates(toMerge, FLAG_MERGE_AT_DEPTH);
			}
		}
	}

	/**
	 * Merge all candidates that are reachable through static fields as a last resort to minimize the number of
	 * parameters.
	 */
	private static void mergeAllStatics(final InterProcCandidateModel pdgModRef, final ModRefCandidateGraph mrg) {
		final List<ModRefRootCandidate> statics = new LinkedList<ModRefRootCandidate>();
		final List<ModRefRootCandidate> others = new LinkedList<ModRefRootCandidate>();

		for (final ModRefRootCandidate r : mrg.getRoots()) {
			if (r.isStatic() != V.NO) {
				statics.add(r);
			} else {
				others.add(r);
			}
		}

		final Set<ModRefCandidate> reachStatic = findReachable(mrg, statics);
		final Set<ModRefCandidate> reachOthers = findReachable(mrg, others);
		reachStatic.removeAll(reachOthers);

		final List<ModRefFieldCandidate> toMerge = new LinkedList<ModRefFieldCandidate>();
		for (final ModRefCandidate c : reachStatic) {
			if (c instanceof ModRefFieldCandidate) {
				toMerge.add((ModRefFieldCandidate) c);
			}
		}

		if (!toMerge.isEmpty()) {
			pdgModRef.mergeCandidates(toMerge, FLAG_MERGE_STATICS);
		}
	}

	private static Set<ModRefCandidate> findReachable(final ModRefCandidateGraph g,
			final List<? extends ModRefCandidate> start) {
		final Set<ModRefCandidate> reachable = new HashSet<ModRefCandidate>(start);
		final LinkedList<ModRefCandidate> work = new LinkedList<ModRefCandidate>();
		work.addAll(start);

		while (!work.isEmpty()) {
			final ModRefCandidate c = work.removeFirst();

			final Iterator<ModRefCandidate> it = g.getSuccNodes(c);
			while (it.hasNext()) {
				final ModRefCandidate succ = it.next();

				if (!reachable.contains(succ)) {
					if (TVL.isFalse(succ.isPrimitive())) {
						work.add(succ);
					}
					reachable.add(succ);
				}
			}
		}

		return reachable;
	}
	
	private Map<ModRefFieldCandidate, Collection<ModRefFieldCandidate>> computeChildrenOf(OrdinalSetMapping<ModRefFieldCandidate> modRefMapping) {
		final Map<ModRefFieldCandidate, Collection<ModRefFieldCandidate>> childrenOf = new HashMap<>(modRefMapping.getSize());
		for (ModRefFieldCandidate candidate : modRefMapping) {
			final Collection<ModRefFieldCandidate> children = new LinkedList<>();
			for (ModRefFieldCandidate other : modRefMapping) {
				if (candidate.isPotentialParentOf(other)) {
					children.add(other);
				}
			}
			childrenOf.put(candidate, new ArrayList<>(children));
		}
		return childrenOf;
	}
	
	private Map<CGNode, OrdinalSet<ModRefFieldCandidate>> simpleReachabilityPropagateWithMerge(
			final CallGraph nonPrunedCG, final CallGraph prunedCG, final ModRefCandidates mrefs,
			final PointsToWrapper pa, final IProgressMonitor progress) throws CancelException {
		// gen-reach in non-pruned callgraph
		final OrdinalSetMapping<ModRefFieldCandidate> modRefMapping;
		final BitVectorSolver<CGNode> solver;
		final BitVectorSolver<CGNode> solverPruned;
		{
			final Map<CGNode, Collection<ModRefFieldCandidate>> intraprocModRef = mrefs.getCandidateMap();
			final Graph<CGNode> cgInverted = GraphInverter.invert(nonPrunedCG);
			final GenReach<CGNode, ModRefFieldCandidate> genReach =
				new GenReach<CGNode, ModRefFieldCandidate>(cgInverted, intraprocModRef);
			solver = new BitVectorSolver<CGNode>(genReach);
			solver.solve(progress);
			modRefMapping = genReach.getLatticeValues();
			
			MonitorUtil.throwExceptionIfCanceled(progress);
			
			// gen-reach in pruned callgraph
			final CallGraph pNoThreadsCG = stripCallsFromThreadStartToRun(prunedCG);
			final Graph<CGNode> cgPrunedInverted = GraphInverter.invert(pNoThreadsCG);
			final GenReach<CGNode, ModRefFieldCandidate> genReachPruned =
				new GenReach<CGNode, ModRefFieldCandidate>(cgPrunedInverted, intraprocModRef, modRefMapping);
			solverPruned = new BitVectorSolver<CGNode>(genReachPruned);
			solverPruned.solve(progress);
		}
		
		final Map<ModRefFieldCandidate, Collection<ModRefFieldCandidate>> childrenOf;
		if (opt.isUseAdvancedInterprocPropagation && opt.isCutOffUnreachable) {
			childrenOf = computeChildrenOf(modRefMapping);
		} else {
			childrenOf = null;
		}
		
		MonitorUtil.throwExceptionIfCanceled(progress);
        int progressCtr = 0;
        if (progress != null) {
            progress.beginTask("simpleReachabilityPropagateMerge", nonPrunedCG.getNumberOfNodes());
        }
        
		final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> result = HashMapFactory.make();
		// check for pruned calls which nodes are at the border.
        final Set<CGNode> borderNodes = findBorderNodes(nonPrunedCG, prunedCG);
        // Make a bitvector of all candidates that exist in the pruned version
        final BitVectorIntSet bvInt = new BitVectorIntSet();
		for (final CGNode cgNode : prunedCG) {
			final BitVectorVariable bv = solverPruned.getOut(cgNode);
			bvInt.addAll(bv.getValue());
		}
        
		for (final CGNode cgNode : nonPrunedCG) {
			// skip results for methods that are not in the pruned cg or not called directly from a
			// method in the pruned cg.
			if (!prunedCG.containsNode(cgNode) && !borderNodes.contains(cgNode)) {
				continue;
			}
			
			final BitVectorVariable bv = solver.getOut(cgNode);
			final IntSet nonPrunedInt = bv.getValue();
			if (nonPrunedInt.isEmpty()) {
				result.put(cgNode, new OrdinalSet<ModRefFieldCandidate>(nonPrunedInt, modRefMapping));
				continue;
			}
			
			// detect unreachable
			final IntSet reachable = (opt.isUseAdvancedInterprocPropagation && opt.isCutOffUnreachable
					? detectReachable(cgNode, nonPrunedInt, modRefMapping, pa, childrenOf)
					: nonPrunedInt);

			// leave only nodes in the set that we are not going to merge
			final IntSet alsoInPruned = reachable.intersection(bvInt);
			final BitVectorIntSet toMergeRef = new BitVectorIntSet();
			final BitVectorIntSet toMergeMod = new BitVectorIntSet();
			
			final InterProcCandidateModel ipcm = mrefs.getCandidates(cgNode);

			// mark all other nodes that are only reachable from non-pruned cg to be merged.
			reachable.foreachExcluding(alsoInPruned, new IntSetAction() {
				@Override
				public void act(final int x) {
					final ModRefFieldCandidate fc = modRefMapping.getMappedObject(x);
					
					// Do not merge nodes with ParameterCandidates that are already present (which are those corresponding to
					// field accesses in this very method!)
					// TODO: this check may be somewhat expensive, but is necessary to establish ModRefFieldCandidate.invariant().
					// Maybe we should reorder merging-due-to-callgraph-prunuing as is done here, and the propgataion 
					// done in fixpointReachabilityPropagate() after all, to get rid of the complicated register-for-merge protocol ?!?!?
					if (ipcm.containsParameterCandidate(fc.pc)) return;

					if (fc.isMod()) {
						toMergeMod.add(x);
					}
					
					if (fc.isRef()) {
						toMergeRef.add(x);
					}
				}
			});
			
			final BitVectorIntSet allNodes = new BitVectorIntSet(reachable);

			// merge nodes that are only reachable from pruned method side-effects
			if (toMergeRef.size() > 1) {
				final OrdinalSet<ModRefFieldCandidate> mRefSet =
					new OrdinalSet<ModRefFieldCandidate>(toMergeRef, modRefMapping);
				toMergeRef.foreach(new IntSetAction() {
					@Override
					public void act(int x) {
						ipcm.addCandidate(modRefMapping.getMappedObject(x));
					}
				});
				ipcm.registerMergeCandidates(mRefSet, FLAG_MERGE_PRUNED_CALL);
			}
			if (toMergeMod.size() > 1) {
				final OrdinalSet<ModRefFieldCandidate> mModSet =
					new OrdinalSet<ModRefFieldCandidate>(toMergeMod, modRefMapping);
				toMergeMod.foreach(new IntSetAction() {
					@Override
					public void act(int x) {
						ipcm.addCandidate(modRefMapping.getMappedObject(x));
					}
				});
				ipcm.registerMergeCandidates(mModSet, FLAG_MERGE_PRUNED_CALL);
			}
			
			result.put(cgNode, new OrdinalSet<ModRefFieldCandidate>(allNodes, modRefMapping));

			MonitorUtil.throwExceptionIfCanceled(progress);
            if (progress != null && progressCtr++ % 103 == 0) { progress.worked(progressCtr); }
		}

        if (progress != null) { progress.done(); }
        
		return result;
	}
	
	private Map<CGNode, OrdinalSet<ModRefFieldCandidate>> simpleReachabilityPropagateNoMerge(
			final CallGraph nonPrunedCG, final CallGraph prunedCG, final ModRefCandidates mrefs,
			final PointsToWrapper pa, final IProgressMonitor progress) throws CancelException {
		// gen-reach in non-pruned callgraph
		final BitVectorSolver<CGNode> solver;
		final OrdinalSetMapping<ModRefFieldCandidate> modRefMapping;
		{
			final Map<CGNode, Collection<ModRefFieldCandidate>> intraprocModRef = mrefs.getCandidateMap();
			final Graph<CGNode> nonPrunedInvCG = GraphInverter.invert(nonPrunedCG);
			final GenReach<CGNode, ModRefFieldCandidate> genReach =
				new GenReach<CGNode, ModRefFieldCandidate>(nonPrunedInvCG, intraprocModRef);
			solver = new BitVectorSolver<CGNode>(genReach);
			solver.solve(progress);
			modRefMapping = genReach.getLatticeValues();
		}
		
		final Map<ModRefFieldCandidate, Collection<ModRefFieldCandidate>> childrenOf;
		if (opt.isUseAdvancedInterprocPropagation && opt.isCutOffUnreachable) {
			childrenOf = computeChildrenOf(modRefMapping);
		} else {
			childrenOf = null;
		}

		
		MonitorUtil.throwExceptionIfCanceled(progress);
		int progressCtr = 0;
        if (progress != null) {
            progress.beginTask("simpleReachabilityPropagateNoMerge", nonPrunedCG.getNumberOfNodes());
        }

		final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> result = HashMapFactory.make();
        // check for pruned calls which nodes are at the border.
        final Set<CGNode> borderNodes = findBorderNodes(nonPrunedCG, prunedCG);
        
		for (final CGNode cgNode : nonPrunedCG) {
			// skip results for methods that are not in the pruned cg or not called directly from a
			// method in the pruned cg.
			if (!prunedCG.containsNode(cgNode) && !borderNodes.contains(cgNode)) {
				continue;
			}
			
			final BitVectorVariable bv = solver.getOut(cgNode);
			final IntSet nonPrunedInt = bv.getValue();
			if (nonPrunedInt.isEmpty()) {
				result.put(cgNode, new OrdinalSet<ModRefFieldCandidate>(nonPrunedInt, modRefMapping));
				continue;
			}
			
			// detect unreachable
			final IntSet reachable = (opt.isUseAdvancedInterprocPropagation && opt.isCutOffUnreachable
					? detectReachable(cgNode, nonPrunedInt, modRefMapping, pa, childrenOf)
					: nonPrunedInt);

			final BitVectorIntSet allNodes = new BitVectorIntSet(reachable);
			result.put(cgNode, new OrdinalSet<ModRefFieldCandidate>(allNodes, modRefMapping));

			MonitorUtil.throwExceptionIfCanceled(progress);
            if (progress != null && progressCtr++ % 103 == 0) { progress.worked(progressCtr); }
		}

        if (progress != null) { progress.done(); }
        
		return result;
	}
	
	
	private Map<CGNode, OrdinalSet<ModRefFieldCandidate>> simpleReachabilityPropagate(final CallGraph cg,
			final CallGraph prunedCg, final ModRefCandidates mrefs, final PointsToWrapper pa,
			final IProgressMonitor progress) throws CancelException {
		if (opt.isMergePrunedCallNodes) {
			return simpleReachabilityPropagateWithMerge(cg, prunedCg, mrefs, pa, progress);
		} else {
			return simpleReachabilityPropagateNoMerge(cg, prunedCg, mrefs, pa, progress);
		}
	}

	/**
	 * Search all nodes in the non-pruned callgraph that are not part of the pruned callgraph, but are called directly
	 * from a node in the pruned callgraph. 
	 */
	private static Set<CGNode> findBorderNodes(final CallGraph nonPrunedCG, final CallGraph prunedCG) {
        final Set<CGNode> borderNodes = new HashSet<CGNode>();
        for (final CGNode n : prunedCG) {
        	for (Iterator<CGNode> it = nonPrunedCG.getSuccNodes(n); it.hasNext();) {
        		final CGNode succ = it.next();
        		if (!prunedCG.containsNode(succ)) {
        			borderNodes.add(succ);
        		}
        	}
        }

        return borderNodes;
	}
	
	private static IntSet detectReachableSlow(final CGNode n, final IntSet candidates,
			final OrdinalSetMapping<ModRefFieldCandidate> map, final PointsToWrapper pa) {
		
		final Set<ModRefRootCandidate> roots = new HashSet<ModRefRootCandidate>();
		final IMethod im = n.getMethod();
		for (int i = 0; i < im.getNumberOfParameters(); i++) {
			final OrdinalSet<InstanceKey> pts = pa.getMethodParamPTS(n, i);
			if (pts != null && !pts.isEmpty()) {
				final PDGNode node = new PDGNode(i, n.getGraphNodeId(), "param " + i, PDGNode.Kind.FORMAL_IN,
						im.getParameterType(i), PDGNode.DEFAULT_NO_LOCAL, PDGNode.DEFAULT_NO_LOCAL);
				node.setBytecodeIndex(BytecodeLocation.ROOT_PARAMETER);
				node.setBytecodeName(BytecodeLocation.getRootParamName(i));
				final ModRefRootCandidate rp = ModRefRootCandidate.createRef(node, pts);
				roots.add(rp);
			}
		}
		
		if (im.getReturnType().isReferenceType()) {
			final OrdinalSet<InstanceKey> pts = pa.getMethodReturnPTS(n);
			if (pts != null && !pts.isEmpty()) {
				final PDGNode node = new PDGNode(-1, n.getGraphNodeId(), "ret", PDGNode.Kind.EXIT,
						im.getReturnType(), PDGNode.DEFAULT_NO_LOCAL, PDGNode.DEFAULT_NO_LOCAL);
				node.setBytecodeIndex(BytecodeLocation.ROOT_PARAMETER);
				node.setBytecodeName(BytecodeLocation.RETURN_PARAM);
				final ModRefRootCandidate rp = ModRefRootCandidate.createRef(node, pts);
				roots.add(rp);
			}
		}
		
		final Set<ModRefFieldCandidate> reachable = new HashSet<ModRefFieldCandidate>();
		final LinkedList<ModRefFieldCandidate> work = new LinkedList<ModRefFieldCandidate>();
		for (IntIterator candIt = candidates.intIteratorSorted(); candIt.hasNext(); ) {
			int x = candIt.next();
			work.add(map.getMappedObject(x));
		}
		
		// add reachable from roots
		LinkedList<ModRefFieldCandidate> newlyAdded = new LinkedList<ModRefFieldCandidate>();
		{
			final int initialSize = work.size();
			for (int i = 0; i < initialSize; i++) {
				final ModRefFieldCandidate toCheck = work.removeFirst();
				for (final ModRefRootCandidate root : roots) {
					if (root.isPotentialParentOf(toCheck)) {
						reachable.add(toCheck);
						newlyAdded.add(toCheck);
						break;
					}
				}
				// add to check later
				work.addLast(toCheck);
			}
		}
		
		while (!newlyAdded.isEmpty()) {
			// use newly added set
			final LinkedList<ModRefFieldCandidate> addedThisTurn = new LinkedList<ModRefFieldCandidate>();
			
			final int initialSize = work.size();
			for (int i = 0; i < initialSize; i++) {
				boolean changed = false;
				final ModRefFieldCandidate toCheck = work.removeFirst();
				
				for (final ModRefFieldCandidate fc : newlyAdded) {
					if (fc.isPotentialParentOf(toCheck)) {
						reachable.add(toCheck);
						addedThisTurn.add(toCheck);
						changed = true;
						break;
					}
				}

				// unchanged - add back to worklist.
				if (!changed) {
					work.addLast(toCheck);
				} 
			}
			newlyAdded = addedThisTurn;
			
			assert (work.size() >= initialSize) == newlyAdded.isEmpty(); // hence: we terminate
		}
		
		final BitVectorIntSet result = new BitVectorIntSet();
		for (final ModRefFieldCandidate fc : reachable) {
			final int id = map.getMappedIndex(fc);
			result.add(id);
		}
		
		return result;
	}
	

	private static IntSet detectReachable(final CGNode n, final IntSet candidates,
			final OrdinalSetMapping<ModRefFieldCandidate> map, final PointsToWrapper pa,
			final Map<ModRefFieldCandidate, Collection<ModRefFieldCandidate>> childrenOf) {
		
		final Set<ModRefRootCandidate> roots = new HashSet<ModRefRootCandidate>();
		final IMethod im = n.getMethod();
		for (int i = 0; i < im.getNumberOfParameters(); i++) {
			final OrdinalSet<InstanceKey> pts = pa.getMethodParamPTS(n, i);
			if (pts != null && !pts.isEmpty()) {
				final PDGNode node = new PDGNode(i, n.getGraphNodeId(), "param " + i, PDGNode.Kind.FORMAL_IN,
						im.getParameterType(i), PDGNode.DEFAULT_NO_LOCAL, PDGNode.DEFAULT_NO_LOCAL);
				node.setBytecodeIndex(BytecodeLocation.ROOT_PARAMETER);
				node.setBytecodeName(BytecodeLocation.getRootParamName(i));
				final ModRefRootCandidate rp = ModRefRootCandidate.createRef(node, pts);
				roots.add(rp);
			}
		}
		
		if (im.getReturnType().isReferenceType()) {
			final OrdinalSet<InstanceKey> pts = pa.getMethodReturnPTS(n);
			if (pts != null && !pts.isEmpty()) {
				final PDGNode node = new PDGNode(-1, n.getGraphNodeId(), "ret", PDGNode.Kind.EXIT,
						im.getReturnType(), PDGNode.DEFAULT_NO_LOCAL, PDGNode.DEFAULT_NO_LOCAL);
				node.setBytecodeIndex(BytecodeLocation.ROOT_PARAMETER);
				node.setBytecodeName(BytecodeLocation.RETURN_PARAM);
				final ModRefRootCandidate rp = ModRefRootCandidate.createRef(node, pts);
				roots.add(rp);
			}
		}
		
		final Map<ModRefFieldCandidate, ModRefFieldCandidate> reachable = new HashMap<>();
		final LinkedList<ModRefFieldCandidate> work = new LinkedList<ModRefFieldCandidate>();
		candidates.foreach(new IntSetAction() {
			@Override
			public void act(final int x) {
				work.add(map.getMappedObject(x));
			}
		});
		
		// add reachable from roots
		LinkedList<ModRefFieldCandidate> newlyAdded = new LinkedList<ModRefFieldCandidate>();
		{
			final int initialSize = work.size();
			for (int i = 0; i < initialSize; i++) {
				final ModRefFieldCandidate toCheck = work.removeFirst();
				for (final ModRefRootCandidate root : roots) {
					if (root.isPotentialParentOf(toCheck)) {
						// TODO: is there a way to do this check in the same hashmap lookup as compute()?!?!
						// I don't really think so :/
						if (!reachable.containsKey(toCheck)) {
							newlyAdded.add(toCheck);
						}
						
						final int toCheckIndex = map.getMappedIndex(toCheck);
						// For *deterministic* behavior *identical* to detectReachableSlow(), for
						// two ModRefFieldCandidate m1, m2 uch that m1.equals(m2),
						// we alywas carry the one with the smaller index in the mapping map.
						// This is correct since in detectReachableSlow():
						//   at each "layer" of the iteration, for two ModRefFieldCandidate m1, m2 
						//   such that m1.equals(m2), either, both or none of the two are added.
						//   Also, there the workList is always ordered in the order of it's initialization,
						//   which is in order of increasing indices.
						reachable.compute(toCheck, (k, canonical) -> {
							if (canonical == null || toCheckIndex < map.getMappedIndex(canonical)) {
								canonical = toCheck;
							}
							return canonical;
						});
						break;
					}
				}
				// add to check later
				work.addLast(toCheck);
			}
		}
		
		while (!newlyAdded.isEmpty()) {
			final ModRefCandidate candidate = newlyAdded.removeFirst();

			for (ModRefFieldCandidate child : childrenOf.get(candidate)) {
				final int childIndex = map.getMappedIndex(child);
				if (candidates.contains(childIndex)) {
					if (!reachable.containsKey(child)) {
						newlyAdded.add(child);
					}
					
					// see above
					reachable.compute(child, (k, canonical) -> {
						if (canonical == null || childIndex < map.getMappedIndex(canonical)) {
							canonical = child;
						}
						return canonical;
					});
				}
			}
		}
		
		final BitVectorIntSet result = new BitVectorIntSet();
		for (final ModRefFieldCandidate fc : reachable.values()) {
			final int id = map.getMappedIndex(fc);
			result.add(id);
		}

		assert            sameAsDetectReachableSlow(new HashSet<>(reachable.values()), n, candidates, map, pa) : "IntSet ==";
		assert result.sameValue(detectReachableSlow(n, candidates, map, pa))                                   : "Set.equals()";
		
		// TODO: we could probably make this whole procedure nicer, if we didn't insist on being "IntSet ==" 
		// with detectReachableSlow(). It should be enough to be both deterministic,
		// and "Set.equals()" with detectReachableSlow().
		return result;
	}
	
	private static boolean sameAsDetectReachableSlow(final Set<ModRefFieldCandidate> reachable, final CGNode n, final IntSet candidates,
			final OrdinalSetMapping<ModRefFieldCandidate> map, final PointsToWrapper pa) {
		final OrdinalSet<ModRefFieldCandidate> resultSlow = new OrdinalSet<>(detectReachableSlow(n, candidates, map, pa), map);
		if (!OrdinalSet.toCollection(resultSlow).equals(reachable)) {
			return false;
		} else {
			return true;
		}

	}
	
	private static class ReachInfo {
		private final CGNode node;
		private final Set<ModRefFieldCandidate> reached = new HashSet<ModRefFieldCandidate>();
		private final Set<ModRefFieldCandidate> unreachable = new HashSet<ModRefFieldCandidate>();
		private OrdinalSet<InstanceKey> reaching;
		private List<ReachInfo> callees = new LinkedList<ReachInfo>();
		private OrdinalSet<ModRefFieldCandidate> pruned = null;
		private boolean changedFromLocal = true;
		private boolean changedFromCallee = false;
		
		private ReachInfo(final CGNode node, final OrdinalSet<InstanceKey> reaching) {
			this.node = node;
			this.reaching = reaching;
		}
		
		public final boolean localPropagate() {
			if (unreachable == null || unreachable.isEmpty()) {
				return false;
			} else if (!(changedFromLocal || changedFromCallee)) {
				return false;
			}
			
			changedFromLocal = false;

			boolean addedCandidates = false;
			boolean change = true;

			while (change) {
				change = false;

				final LinkedList<ModRefFieldCandidate> added = new LinkedList<ModRefFieldCandidate>();
				
				for (final ModRefFieldCandidate f : unreachable) {
					if (f.isReachableFrom(reaching)) {
						change = true;
						reaching = unify(reaching, f.pc.getFieldPointsTo());
						added.add(f);
					}
				}

				if (change) {
					addedCandidates = true;

					for (final ModRefFieldCandidate f : added) {
						unreachable.remove(f);
						reached.add(f);
					}
				}
			}

			changedFromLocal = addedCandidates;
			
			return addedCandidates;
		}

		private boolean firstRun = true;
		
		public final boolean propagateFromCallees() {
			boolean changed = false;
			
			for (final ReachInfo callee : callees) {
				if (!(changed || changedFromLocal || changedFromCallee
						|| callee.changedFromLocal || callee.changedFromCallee)) {
					continue;
				}
				
				for (final ModRefFieldCandidate c : callee.reached) {
					if (!reached.contains(c) && c.isReachableFrom(reaching)) {
						changed = true;
						reached.add(c);
						reaching = unify(reaching, c.pc.getFieldPointsTo());
					}
				}
			}
			
			if (pruned != null && (firstRun || changed || changedFromLocal || changedFromCallee)) {
				for (final ModRefFieldCandidate c : pruned) {
					if (!reached.contains(c) && c.isReachableFrom(reaching)) {
						changed = true;
						reached.add(c);
						reaching = unify(reaching, c.pc.getFieldPointsTo());
					}
				}
			}
			
			changedFromCallee = changed;
			firstRun = false;
			
			return changed;
		}
	}
	
	private static boolean USE_NORMAL_PRUNED_SE = true;
	
	private Map<CGNode, OrdinalSet<ModRefFieldCandidate>> fixpointReachabilityPropagate(final SDGBuilder sdg,
			final CallGraph nonPrunedCG, final ModRefCandidates mrefs, final IProgressMonitor progress)
	throws CancelException {
		final Map<CGNode, ReachInfo> cg2reach = new HashMap<CGNode, ReachInfo>();
		final CallGraph prunedCG = sdg.getWalaCallGraph();
		final PointsToWrapper pa = new PointsToWrapper(sdg.getPointerAnalysis());

		final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> simple =
				simpleReachabilityPropagate(nonPrunedCG, prunedCG, mrefs, pa, progress);

        int progressCtr = 0;
        if (progress != null) {
            progress.beginTask("fixpointReachabilityPropagate", IProgressMonitor.UNKNOWN);
        }
		// init with roots
		final Map<CGNode, Collection<ModRefFieldCandidate>> cg2localfields = mrefs.getCandidateMap();
		for (final PDG pdg : sdg.getAllPDGs()) {
			final CGNode n = pdg.cgNode;
			final OrdinalSet<InstanceKey> initialRoot =
					ModRefCandidateGraph.findMethodRootPts(pa, pdg, mrefs.ignoreExceptions);
			final ReachInfo reach = new ReachInfo(n, initialRoot);
			final Collection<ModRefFieldCandidate> locals = cg2localfields.get(n);
			if (locals != null) {
				for (ModRefFieldCandidate c : locals) {
					assert !c.pc.isMerged();
					reach.unreachable.add(c);
				}
			}
			cg2reach.put(n, reach);
		}

		// prepare dfs finish time sorted list for fixpoint iteration.
		final LinkedList<CGNode> dfsFinish = new LinkedList<CGNode>();
		{
			final DFSFinishTimeIterator<CGNode> it = DFS.iterateFinishTime(prunedCG);
			while (it.hasNext()) {
				final CGNode n = it.next();
				if (sdg.getPDGforMethod(n) != null) {
					dfsFinish.add(n);
				}
			}
		}
        if (progress != null) { progress.worked(progressCtr++); }

		// init reach info callees
		for (final CGNode n : dfsFinish) {
			final ReachInfo reach = cg2reach.get(n);
			for (final Iterator<CGNode> it = nonPrunedCG.getSuccNodes(n); it.hasNext();) {
				final CGNode succ = it.next();
				final ReachInfo succReach = cg2reach.get(succ);
				
				if (succReach != null) {
					reach.callees.add(succReach);
				} else {
					// pruned call
					final OrdinalSet<ModRefFieldCandidate> prunedSucc = simple.get(succ);
					// check if is escaping && merge before propagation
					if (USE_NORMAL_PRUNED_SE) {
						reach.pruned = unify(reach.pruned, prunedSucc);
					} else {
						final PDG callerPDG = sdg.getPDGforMethod(n);
						final Iterator<CallSiteReference> csrs = nonPrunedCG.getPossibleSites(n, succ);
						//callerPDG.
						while (csrs.hasNext()) {
							final CallSiteReference csr = csrs.next();
							final IR ir = n.getIR();
							final SSAInstruction[] instrs = ir.getInstructions();
							final IntSet calli = ir.getCallInstructionIndices(csr);
							calli.foreach(new IntSetAction() {
	
								@Override
								public void act(final int index) {
									final SSAInstruction i = instrs[index];
									if (i == null) {
										return;
									}
									final PDGNode calln = callerPDG.getNode(i);
									if (calln == null) {
										return;
									}
									final OrdinalSet<InstanceKey> roots = ModRefCandidateGraph.findCallRootsPts(pa,
										callerPDG, calln, mrefs.ignoreExceptions);
									if (roots != null && !roots.isEmpty()) {
										final OrdinalSet<ModRefFieldCandidate> reachable =
											filterUnreachable(roots, prunedSucc);
										reach.pruned = unify(reach.pruned, reachable);
									}
								}
								
							});
						}
						
						if (reach.pruned == null) {
							reach.pruned = prunedSucc;
						}
					}
				}
			}
		}
        if (progress != null) { progress.worked(progressCtr++); }
		
		boolean changed = true;
		while (changed) {
			changed = false;
			
			for (final CGNode n : dfsFinish) {
				final ReachInfo callerReach = cg2reach.get(n);
				changed |= callerReach.localPropagate();

				boolean changeN = true;
				while (changeN) {
					changeN = callerReach.propagateFromCallees();
					changed |= changeN;
				}
			}

            if (progress != null) { progress.worked(progressCtr++); }
		}

		final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> result = convertResult(cg2reach, simple, mrefs);

		/*
		 * TODO: find out why the following assertion does not hold (e.g.: run LibraryPruningTest.testGuiPruneExtended() ).
		for (Entry<CGNode, OrdinalSet<ModRefFieldCandidate>> entry : result.entrySet()) {
			final OrdinalSet<ModRefFieldCandidate> resultSet = entry.getValue();
			final OrdinalSet<ModRefFieldCandidate> simpleSet = simple.get(entry.getKey());
			for (ModRefFieldCandidate c : resultSet) {
				assert simpleSet.contains(c);
			}
		}
		*/
		
        if (progress != null) { progress.done(); }
        
		return result;
	}
	
	private static OrdinalSet<ModRefFieldCandidate> filterUnreachable(final OrdinalSet<InstanceKey> roots,
			final OrdinalSet<ModRefFieldCandidate> cands) {
		OrdinalSet<InstanceKey> reachablePts = roots;
		final MutableIntSet reach = IntSetUtil.make();
		final MutableIntSet check = IntSetUtil.makeMutableCopy(reachablePts.getBackingSet());
		final OrdinalSetMapping<ModRefFieldCandidate> mapping = cands.getMapping();
		final OrdinalSet<ModRefFieldCandidate> reachSet = new OrdinalSet<ModRefFieldCandidate>(reach, mapping);
		final OrdinalSet<ModRefFieldCandidate> checkSet = new OrdinalSet<ModRefFieldCandidate>(check, mapping);
		
		boolean changed = true;
		while (changed) {
			changed = false;
			for (final ModRefFieldCandidate c : checkSet) {
				if (!reachSet.contains(c)) {
					if (c.isReachableFrom(reachablePts)) {
						changed = true;
						reach.add(mapping.getMappedIndex(c));
					} else {
						for (final ModRefFieldCandidate r : reachSet) {
							if (r.isPotentialParentOf(c)) {
								changed = true;
								reach.add(mapping.getMappedIndex(c));
							}
						}
					}
				}
			}
		}
		
		return new OrdinalSet<ModRefFieldCandidate>(reach, mapping);
	}

	private static Map<CGNode, OrdinalSet<ModRefFieldCandidate>> convertResult(
			final Map<CGNode, ReachInfo> cg2reach,	final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> simple,
			final ModRefCandidates mrefs) {
		final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> result =
				new HashMap<CGNode, OrdinalSet<ModRefFieldCandidate>>();
		final OrdinalSetMapping<ModRefFieldCandidate> domain;
		if (simple.isEmpty()) {
			domain = createMapping(cg2reach);
		} else {
			domain = simple.values().iterator().next().getMapping();
			assert domain != null;
		}

		for (final ReachInfo reach : cg2reach.values()) {
			final BitVectorIntSet set = new BitVectorIntSet();
			for (final ModRefFieldCandidate c : reach.reached) {
				final int id = domain.getMappedIndex(c);
				set.add(id);
			}

			final OrdinalSet<ModRefFieldCandidate> cands;
			if (set.isEmpty()) {
				cands = OrdinalSet.empty();
			} else {
				cands = new OrdinalSet<ModRefFieldCandidate>(set, domain);
			}

			result.put(reach.node, cands);
			
			if (!reach.unreachable.isEmpty()) {
				final InterProcCandidateModel interCands = mrefs.getCandidates(reach.node);
				for (final ModRefFieldCandidate fc : reach.unreachable) {
					if (interCands.contains(fc)) {
						interCands.removeCandidate(fc);
					}
				}
			}
		}

		for (final Entry<CGNode, OrdinalSet<ModRefFieldCandidate>> e : simple.entrySet()) {
			if (!result.containsKey(e.getKey())) {
				final OrdinalSet<ModRefFieldCandidate> simpleSet = simple.get(e.getKey());
				result.put(e.getKey(), simpleSet);
			}
		}

		return result;
	}

	private static OrdinalSetMapping<ModRefFieldCandidate> createMapping(
			final Map<CGNode, ReachInfo> cg2reach) {
	    final MutableMapping<ModRefFieldCandidate> result = MutableMapping.make();

	    for (final ReachInfo reach : cg2reach.values()) {
	    	for (final ModRefFieldCandidate c : reach.reached) {
    			result.add(c);
	    	}
	    }

	    return result;
	}

	private CallGraph stripCallsFromThreadStartToRun(final CallGraph cg) {
		return NoThreadStartToRunCallGraph.create(cg);
	}
}
