/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
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
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.util.Config;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.ParameterFieldFactory;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.dataflow.GenReach;
import edu.kit.joana.wala.core.params.objgraph.ModRefCandidates.InterProcCandidateModel;
import edu.kit.joana.wala.core.params.objgraph.TVL.V;
import edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory;
import edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactoryImpl;
import edu.kit.joana.wala.core.params.objgraph.candidates.MergeByPartition;
import edu.kit.joana.wala.core.params.objgraph.candidates.MergeStrategy;
import edu.kit.joana.wala.core.params.objgraph.dataflow.ModRefDataFlow;
import edu.kit.joana.wala.core.params.objgraph.dataflow.PointsToWrapper;
import edu.kit.joana.wala.util.PrettyWalaNames;

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
			this.isCutOffUnreachable = true;
			this.isMergeException = true;
			this.isCutOffImmutables = true;
			this.isMergeOneFieldPerParent = true;
			this.isMergePrunedCallNodes = true;
			this.isUseAdvancedInterprocPropagation = true;
			this.maxNodesPerInterface = DEFAULT_MAX_NODES_PER_INTERFACE;
			this.convertToObjTree = false;
			this.doStaticFields = false;
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
		 * Use advanced (and slower fixed point based) interprocedural propagation with reachability check during propagation.
		 */
		public boolean isUseAdvancedInterprocPropagation;

		/**
		 * Maximal number of nodes for a single method interface (formal-nodes). If more nodes are part
		 * of the interface, they will be merged.
		 */
		public int maxNodesPerInterface;

		public static final int UNLIMITED_NODES_PER_INTERFACE = -1;
		public static final int DEFAULT_MAX_NODES_PER_INTERFACE = 30;
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
		 */
		public static final int STD_MERGE_LEVEL = 3;

	}

	private ObjGraphParams(final SDGBuilder sdg, final Options opt) {
		this.sdg = sdg;
		this.opt = opt;
		this.opt.adjustWithProperties();
	}
	
	private void run(final IProgressMonitor progress) throws CancelException {
		final Logger logStats = Log.getLogger(Log.L_OBJGRAPH_STATS);
		sdg.cfg.out.print("(if");

		// step 1 create candidates
		final CallGraph cg = stripCallsFromThreadStartToRun(sdg.getNonPrunedWalaCallGraph());
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
		final ModRefCandidates mrefs = ModRefCandidates.computeIntracProc(pfact, candFact, cg,
				sdg.getPointerAnalysis(), opt.doStaticFields, progress);

		sdg.cfg.out.print("1");

		// create side-effect detector if command line option has been set.
		if (sdg.cfg.sideEffects == null && SideEffectDetectorConfig.isActivated()) {
			sdg.cfg.sideEffects = SideEffectDetectorConfig.maybeCreateInstance();
		}
		
		if (sdg.cfg.sideEffects != null) {
			sdg.cfg.sideEffects.copyIntraprocState(mrefs);
		}
		
		// step 2 propagate interprocedural
		long t1 = 0, t2 = 0;
		if (logStats.isEnabled()) { t1 = System.currentTimeMillis(); }

		final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> interModRef;
		if (opt.isUseAdvancedInterprocPropagation) {
			interModRef = fixpointReachabilityPropagate(sdg, cg, mrefs, progress);
		} else {
			interModRef = simpleReachabilityPropagate(cg, mrefs, progress);
		}

		sdg.cfg.out.print("8");

		long sdgNodeCount = 0;
		if (logStats.isEnabled()) {
			t2 = System.currentTimeMillis();
			sdgNodeCount = sdg.countNodes();
		}

		sdg.cfg.out.print("9");

		adjustInterprocModRef(cg, interModRef, mrefs, sdg, progress);

		sdg.cfg.out.print("-");

		if (sdg.cfg.sideEffects != null) {
			// detect modifications to a given pointerkey
			sdg.cfg.out.print(",se");
			sdg.cfg.sideEffects.runAnalysis(sdg, cg, mrefs, progress);
			// free memory
			sdg.cfg.sideEffects = null;
		}
		
		sdg.cfg.out.print(",df");

		ModRefDataFlow.compute(mrefs, sdg, progress);

		if (sdg.cfg.localKillingDefs) {
			sdg.registerFinalModRef(mrefs, progress);
			sdg.cfg.out.print(",reg");
		}

		long sdgNodeCountGraph = 0;
		if (opt.convertToObjTree) {
			sdg.cfg.out.print(",2tree");

			if (logStats.isEnabled()) {
				sdgNodeCountGraph = sdg.countNodes();
			}

			ObjTreeConverter.convert(sdg, progress);
		}

		sdg.cfg.out.print(")");

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

	private void adjustInterprocModRef(final CallGraph cg, final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> interModRef,
			final ModRefCandidates modref, final SDGBuilder sdg, final IProgressMonitor progress) throws CancelException {
		// add all nodes to the interface
		for (final CGNode n : cg) {
			MonitorUtil.throwExceptionIfCanceled(progress);
			final OrdinalSet<ModRefFieldCandidate> pimod = interModRef.get(n);

			if (!pimod.isEmpty()) {
				final InterProcCandidateModel interCands = modref.getCandidates(n);

				for (final ModRefFieldCandidate c : pimod) {
					interCands.addCandidate(c);
				}
			}
		}

		sdg.cfg.out.print("a");
		
		if (opt.isCutOffUnreachable || opt.isMergeException || opt.isCutOffImmutables || opt.isMergeOneFieldPerParent) {
			final PointsToWrapper pa = new PointsToWrapper(sdg.getPointerAnalysis());

			for (final PDG pdg : sdg.getAllPDGs()) {
				MonitorUtil.throwExceptionIfCanceled(progress);
				final ModRefCandidateGraph mrg = ModRefCandidateGraph.compute(pa, modref, pdg);
				final InterProcCandidateModel pdgModRef = modref.getCandidates(pdg.cgNode);

				sdg.cfg.out.println(PrettyWalaNames.methodName(pdg.getMethod()));

				if (pdgModRef == null) { continue; }

				// cut off unreachable nodes
				if (opt.isCutOffUnreachable) {
					sdg.cfg.out.print("a[1");
					cutOffUnreachable(pdgModRef, mrg, mrg.getRoots());
					sdg.cfg.out.print("]");
				}

				// merge nodes only reachable from exceptions
				if (opt.isMergeException) {
					sdg.cfg.out.print("a[2");
					mergeException(pdgModRef, mrg);
					sdg.cfg.out.print("]");
				}

				// cut off fields of immutables
				if (opt.isCutOffImmutables) {
					sdg.cfg.out.print("a[3");
					cutOffImmutables(pdgModRef, mrg, pdg);
					sdg.cfg.out.print("]");
				}

				if (opt.isMergeOneFieldPerParent) {
					sdg.cfg.out.print("a[4");
					mergeOneFieldPerParent(pdgModRef, mrg);
					sdg.cfg.out.print("]");
				}

				if (opt.maxNodesPerInterface != Options.UNLIMITED_NODES_PER_INTERFACE
						&& opt.maxNodesPerInterface < pdgModRef.size()) {
					//System.err.print(pdg + ": " + pdgModRef.size());

					for (int level = Options.STD_MERGE_LEVEL; level > 0
							&& pdgModRef.size() > opt.maxNodesPerInterface; level--) {
						sdg.cfg.out.print("a[5");

						mergeAtLevel(pdgModRef, mrg, level);
						//System.err.print(" -(" + level + ")-> " + pdgModRef.size());
						sdg.cfg.out.print("]");

					}

					if (pdgModRef.size() > opt.maxNodesPerInterface) {
						sdg.cfg.out.print("a[6");

						// if the interface is still too big, we try to merge all locations only reachable through
						// static fields to a single candidate.
						mergeAllStatics(pdgModRef, mrg);
						//System.err.print(" -(S)-> " + pdgModRef.size());
						sdg.cfg.out.print("]");
					}

					//System.err.println(" done.");
				}
			}
		}

		sdg.cfg.out.print("b");

		// merge nodes for cut off cgnodes
		if (opt.isMergePrunedCallNodes) {
			mergePrunedCallNodes(sdg, cg, interModRef, modref, progress);
		}
		
		sdg.cfg.out.print("c");

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
			pdgModRef.mergeCandidates(toMerge);
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

				pdgModRef.mergeCandidates(toMerge);
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

			final OrdinalSet<ParameterField> fields = succ.getFields();

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
				pdgModRef.mergeCandidates(cands);
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
				interCands.mergeCandidates(pimod);
			}
		}
	}

	private static void mergeAtLevel(final InterProcCandidateModel pdgModRef, final ModRefCandidateGraph mrg,
			final int level) {
		assert level >= 0;

		final List<ModRefCandidate> start = new LinkedList<ModRefCandidate>();
		for (final ModRefCandidate c : mrg.getRoots()) {
			start.add(c);
		}
		final Set<ModRefCandidate> reachable = new HashSet<ModRefCandidate>();
		final List<ModRefCandidate> border = new LinkedList<ModRefCandidate>();

		for (int i = level; i > 0; i--) {
			final Set<ModRefCandidate> succs = findDirectSuccReachable(mrg, start);

			if (i == 1) {
				for (final ModRefCandidate c : succs) {
					if (!reachable.contains(c)) {
						border.add(c);
					}
				}
			}

			reachable.addAll(succs);
			start.clear();
			start.addAll(succs);
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
				pdgModRef.mergeCandidates(toMerge);
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

		final List<ModRefFieldCandidate> toMerge = new LinkedList<ModRefFieldCandidate>();
		for (final ModRefCandidate c : reachStatic) {
			if (c instanceof ModRefFieldCandidate) {
				toMerge.add((ModRefFieldCandidate) c);
			}
		}

		if (!toMerge.isEmpty()) {
			pdgModRef.mergeCandidates(toMerge);
		}
	}

	private static Set<ModRefCandidate> findDirectSuccReachable(final ModRefCandidateGraph g,
			final List<? extends ModRefCandidate> start) {
		final Set<ModRefCandidate> reach = new HashSet<ModRefCandidate>();

		for (final ModRefCandidate c : start) {
			final Iterator<ModRefCandidate> it = g.getSuccNodes(c);
			while (it.hasNext()) {
				final ModRefCandidate succ = it.next();
				reach.add(succ);
			}
		}

		return reach;
	}

	private static Set<ModRefCandidate> findReachable(final ModRefCandidateGraph g,
			final List<? extends ModRefCandidate> start) {
		final Set<ModRefCandidate> reachable = new HashSet<ModRefCandidate>();
		reachable.addAll(start);
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

	private Map<CGNode, OrdinalSet<ModRefFieldCandidate>> simpleReachabilityPropagate(final CallGraph cg,
			final ModRefCandidates mrefs, final IProgressMonitor progress) throws CancelException {
		final Graph<CGNode> cgInverted = GraphInverter.invert(cg);
		sdg.cfg.out.print("2");

		final Map<CGNode, Collection<ModRefFieldCandidate>> gen = mrefs.getCandidateMap();
		sdg.cfg.out.print("3");
		MonitorUtil.throwExceptionIfCanceled(progress);
		final GenReach<CGNode, ModRefFieldCandidate> genReach = new GenReach<CGNode, ModRefFieldCandidate>(cgInverted, gen);
		sdg.cfg.out.print("4");

		final BitVectorSolver<CGNode> solver = new BitVectorSolver<CGNode>(genReach);
		sdg.cfg.out.print("5");

		solver.solve(progress);
		sdg.cfg.out.print("6");

		final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> result = HashMapFactory.make();
		for (final CGNode cgNode : cg) {
			final BitVectorVariable bv = solver.getOut(cgNode);
			result.put(cgNode, new OrdinalSet<ModRefFieldCandidate>(bv.getValue(), genReach.getLatticeValues()));
			MonitorUtil.throwExceptionIfCanceled(progress);
		}
		sdg.cfg.out.print("7");

		return result;
	}

	private Map<CGNode, OrdinalSet<ModRefFieldCandidate>> fixpointReachabilityPropagate(final SDGBuilder sdg,
			final CallGraph nonPrunedCG, final ModRefCandidates mrefs, final IProgressMonitor progress)
	throws CancelException {
		final Map<CGNode, Set<ModRefCandidate>> cg2candidates = new HashMap<CGNode, Set<ModRefCandidate>>();
		final CallGraph prunedCG = sdg.getWalaCallGraph();
		final PointsToWrapper pa = new PointsToWrapper(sdg.getPointerAnalysis());
		final Map<CGNode, Collection<ModRefFieldCandidate>> cg2localfields = mrefs.getCandidateMap();

		final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> simple =
				simpleReachabilityPropagate(nonPrunedCG, mrefs, progress);

		sdg.cfg.out.print("z");
		
		// init with roots
		for (final PDG pdg : sdg.getAllPDGs()) {
			final List<ModRefRootCandidate> roots = ModRefCandidateGraph.findMethodRoots(pa, pdg);
			final Set<ModRefCandidate> cands = new HashSet<ModRefCandidate>();
			cands.addAll(roots);
			cg2candidates.put(pdg.cgNode, cands);
			localPropagate(cands, cg2localfields.get(pdg.cgNode));
		}

		sdg.cfg.out.print("y");

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
		boolean changed = true;
		while (changed) {
			changed = false;
			
			for (final CGNode n : dfsFinish) { 
				final Set<ModRefCandidate> caller = cg2candidates.get(n);
				final Collection<ModRefFieldCandidate> fields = cg2localfields.get(n);
				if (fields != null && !fields.isEmpty()) {
					changed |= localPropagate(caller, fields);
				}

				final List<Set<ModRefCandidate>> calleeCands = findAllCalleeCandidates(n, cg2candidates, prunedCG,
						simple, nonPrunedCG);
				boolean changeN = true;

				while (changeN) {
					changeN = false;

					for (final Set<ModRefCandidate> callee : calleeCands) {
						changeN |= propagate(caller, callee);
					}

					changed |= changeN;
				}
			}
		}

		sdg.cfg.out.print("x");
		
		final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> result = convertResult(cg2candidates, simple);

		sdg.cfg.out.print("w");
		
		return result;
	}

	private static boolean localPropagate(final Set<ModRefCandidate> cands, final Collection<ModRefFieldCandidate> fields) {
		if (fields == null || fields.isEmpty()) {
			return false;
		}

		boolean addedCandidates = false;
		boolean change = true;

		while (change) {
			change = false;

			final List<ModRefFieldCandidate> added = new LinkedList<ModRefFieldCandidate>();

			for (final ModRefFieldCandidate f : fields) {
				boolean addF = false;

				for (final ModRefCandidate c : cands) {
					if (c.isPotentialParentOf(f)) {
						addF = true;
						break;
					}
				}

				if (addF) {
					cands.add(f);
					added.add(f);
				}
			}

			if (!added.isEmpty()) {
				change = true;
				addedCandidates = true;
				for (final ModRefFieldCandidate f : added) {
					fields.remove(f);
				}
			}
		}

		return addedCandidates;
	}

	private static Map<CGNode, OrdinalSet<ModRefFieldCandidate>> convertResult(
			final Map<CGNode, Set<ModRefCandidate>> cg2candidates,
			final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> simple) {
		final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> result =
				new HashMap<CGNode, OrdinalSet<ModRefFieldCandidate>>();
		final OrdinalSetMapping<ModRefFieldCandidate> domain;
		if (simple.isEmpty()) {
			domain = createMapping(cg2candidates);
		} else {
			domain = simple.values().iterator().next().getMapping();
			assert domain != null;
		}

		for (final Entry<CGNode, Set<ModRefCandidate>> e : cg2candidates.entrySet()) {
			final BitVectorIntSet set = new BitVectorIntSet();
			for (final ModRefCandidate c : e.getValue()) {
				if (c instanceof ModRefFieldCandidate) {
					final int id = domain.getMappedIndex((ModRefFieldCandidate) c);
					set.add(id);
				}
			}

			final OrdinalSet<ModRefFieldCandidate> cands;
			if (set.isEmpty()) {
				cands = OrdinalSet.empty();
			} else {
				cands = new OrdinalSet<ModRefFieldCandidate>(set, domain);
			}

			result.put(e.getKey(), cands);
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
			final Map<CGNode, Set<ModRefCandidate>> cg2candidates) {
	    final MutableMapping<ModRefFieldCandidate> result = MutableMapping.make();

	    for (final Set<ModRefCandidate> cands : cg2candidates.values()) {
	    	for (final ModRefCandidate c : cands) {
	    		if (c instanceof ModRefFieldCandidate) {
	    			result.add((ModRefFieldCandidate) c);
	    		}
	    	}
	    }

	    return result;
	}

	private static List<Set<ModRefCandidate>> findAllCalleeCandidates(final CGNode n,
			final Map<CGNode, Set<ModRefCandidate>> cg2candidates, final CallGraph pruned,
			final Map<CGNode, OrdinalSet<ModRefFieldCandidate>> simple, final CallGraph nonPruned) {
		final List<Set<ModRefCandidate>> list = new LinkedList<Set<ModRefCandidate>>();

		final Iterator<CGNode> succIt = nonPruned.getSuccNodes(n);
		while (succIt.hasNext()) {
			final CGNode callee = succIt.next();

			if (!cg2candidates.containsKey(callee)) {
				// use simple propagation result
				final OrdinalSet<ModRefFieldCandidate> cands = simple.get(callee);
				final Set<ModRefCandidate> cSet = new HashSet<ModRefCandidate>();
				for (final ModRefFieldCandidate f : cands) {
					cSet.add(f);
				}
				cg2candidates.put(callee, cSet);
				list.add(cSet);
			} else {
				// use result from map
				final Set<ModRefCandidate> cSet = cg2candidates.get(callee);
				assert cSet != null;
				list.add(cSet);
			}
		}

		return list;
	}

	private static boolean propagate(final Set<ModRefCandidate> caller, final Set<ModRefCandidate> callee) {
		boolean changed = false;

		for (final ModRefCandidate c : callee) {
			if (c instanceof ModRefFieldCandidate && !caller.contains(c)) {
				final ModRefFieldCandidate f = (ModRefFieldCandidate) c;
				final List<ModRefFieldCandidate> toAdd = new LinkedList<ModRefFieldCandidate>();

				for (final ModRefCandidate parent : caller) {
					if (parent.isPotentialParentOf(f)) {
						toAdd.add(f);
					}
				}

				if (toAdd.size() > 0) {
					changed = true;
					caller.addAll(toAdd);
				}
			}
		}

		return changed;
	}

	private CallGraph stripCallsFromThreadStartToRun(final CallGraph cg) {
		return NoThreadStartToRunCallGraph.create(cg);
	}
}
