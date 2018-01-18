/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.dataflow;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

import edu.kit.joana.util.Pair;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.params.objgraph.ModRefCandidateGraph;
import edu.kit.joana.wala.core.params.objgraph.ModRefCandidates;
import edu.kit.joana.wala.core.params.objgraph.ModRefFieldCandidate;
import edu.kit.joana.wala.core.params.objgraph.ModRefRootCandidate;
import edu.kit.joana.wala.core.params.objgraph.candidates.CandidateFactory;
import edu.kit.joana.wala.core.params.objgraph.candidates.ParameterCandidate;
import edu.kit.joana.wala.core.params.objgraph.candidates.UniqueParameterCandidate;
import edu.kit.joana.wala.core.params.objgraph.dataflow.ModRefControlFlowGraph.Node;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class ModRefDataFlow {

	public static void compute(final ModRefCandidates modref, final SDGBuilder sdg, final IProgressMonitor progress)
			throws CancelException {
		final ModRefDataFlow df = new ModRefDataFlow();
		df.run(modref, sdg, progress);
	}

	private ModRefDataFlow() {}

	private void run(final ModRefCandidates modref, final SDGBuilder sdg, final IProgressMonitor progress)
			throws CancelException {
		final CallGraph cg = sdg.getNonPrunedWalaCallGraph();
        int progressCtr = 0;
        if (progress != null) {
            progress.beginTask("ModRef Dataflow", sdg.getAllPDGs().size());
        }

		// saves mapping, so we can create parameter-in and -out edges afterwards
		final TIntObjectHashMap<ModRefFieldCandidate> pdgnode2modref =
				new TIntObjectHashMap<ModRefFieldCandidate>();

		// a pre-computation of mayAliasing information. run-time wise, this
		// does seem to pay off nicely.
		// TODO: what is the memory-impact of this?
		// TODO: can we nicely parallelize this?
		final CandidateFactory candFact = modref.getCandFact();
		final Map<UniqueParameterCandidate, Set<UniqueParameterCandidate>> mayAliased = new HashMap<>();
		int toSkip = 0;
		for (UniqueParameterCandidate u1 : candFact.getUniqueCandidates()) {
			int skipped = 0;
			for (UniqueParameterCandidate u2 : candFact.getUniqueCandidates()) {
				if (skipped++ < toSkip) continue;
				if (u1.isMayAliased(u2)) {
					mayAliased.compute(u1, (k, aliased) -> {
						if (aliased == null) {
							aliased = new HashSet<>();
						}
						aliased.add(u2);
						return aliased;
					});
					mayAliased.compute(u2, (k, aliased) -> {
						if (aliased == null) {
							aliased = new HashSet<>();
						}
						aliased.add(u1);
						return aliased;
					});
				}
				
			}
			toSkip++;
		}
		
		
		Stream<PDG> s = sdg.isParallel()?sdg.getAllPDGs().parallelStream():sdg.getAllPDGs().stream();
		s.forEach(pdg -> {
//			MonitorUtil.throwExceptionIfCanceled(progress);
//            if (progress != null) {
//                progress.subTask(pdg.getMethod().toString());
//                progress.worked(progressCtr++);
//            }

			final ModRefControlFlowGraph cfg = ModRefControlFlowGraph.compute(modref, pdg, cg, progress);

			final OrdinalSetMapping<Node> domain = ModRefProviderImpl.createDomain(cfg);
			final ModRefProvider provider = ModRefProviderImpl.createProvider(domain, sdg.isParallel());
			final ITransferFunctionProvider<Node, BitVectorVariable> transfer =
					new ModRefReachingDefTransferFunctions(domain, provider);
			final BitVectorFramework<Node, Node> reachDef =	new BitVectorFramework<Node, Node>(cfg, transfer, domain);
			final BitVectorSolver<Node> solver = new BitVectorSolver<Node>(reachDef);

			try {
				solver.solve(progress);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			final Map<Node, PDGNode> node2pdg;
			final Map<UniqueParameterCandidate, Set<Node>> uniqueParameterCandidate2Node;
			{
				final Pair<Map<Node, PDGNode>, Map<UniqueParameterCandidate, Set<Node>>> pair = createPDGNodes(cfg, pdg, pdgnode2modref);
				node2pdg = pair.getFirst();
				uniqueParameterCandidate2Node = pair.getSecond();
			}

			final Map<Node, OrdinalSet<Node>> mayRead = computeLastReachingDefs(
					cfg.getRefs(),
					mayAliased,
					uniqueParameterCandidate2Node,
					solver, domain, provider, sdg.isParallel()
			);
			
			assert isSameMayRead(
				computeLastReachingDefsSlow(solver, domain, provider, sdg.isParallel()),
				mayRead,
				domain
			);

			addDataDepEdgesToPDG(node2pdg, mayRead, pdg);
			
		});
		
		mayAliased.clear();

		connectFormalAndActualParams(sdg, pdgnode2modref);
		connectParameterStructure(sdg, pdgnode2modref);
		if (sdg.cfg.parameterPTSConsumer != null) sdg.cfg.parameterPTSConsumer.commitPDGNode2ModRefMapping(pdgnode2modref);
        if (progress != null) progress.done();
	}
	
	private static boolean isSameMayRead(
		Map<Node, OrdinalSet<Node>> mayReadSlow,
		Map<Node, OrdinalSet<Node>> mayRead,
		OrdinalSetMapping<Node> domain
	) {
		if (!mayReadSlow.keySet().containsAll(mayRead.keySet())) {
			return false;
		}
		final OrdinalSet<Node> empty = new OrdinalSet<>(EmptyIntSet.instance, domain);
		for (Entry<Node, OrdinalSet<Node>> e : mayReadSlow.entrySet()) {
			final Node n = e.getKey();
			final OrdinalSet<Node> slow   = e.getValue();
			final OrdinalSet<Node> normal = mayRead.getOrDefault(n, empty);
			if (!OrdinalSet.equals(slow, normal)) {
				return false;
			}
		}
		return true;
	}
	

	private static void connectParameterStructure(final SDGBuilder sdg,
			final TIntObjectHashMap<ModRefFieldCandidate> pdgnode2modref) {
		final PointsToWrapper pa = new PointsToWrapper(sdg.getPointerAnalysis());
		final boolean ignoreExceptions = sdg.cfg.exceptions == ExceptionAnalysis.IGNORE_ALL;

		for (final PDG pdg : sdg.getAllPDGs()) {
			{
				final List<PDGNode> formals = new LinkedList<PDGNode>();
				for (final PDGEdge e : pdg.outgoingEdgesOf(pdg.entry)) {
					if (e.kind == PDGEdge.Kind.CONTROL_DEP_EXPR
							&& (e.to.getKind() == PDGNode.Kind.FORMAL_IN || e.to.getKind() == PDGNode.Kind.FORMAL_OUT)
							&& pdgnode2modref.contains(e.to.getId())) {
						formals.add(e.to);
					}
				}

				connectParameterStructureNodes(pdg, pdgnode2modref, formals);
				final List<ModRefRootCandidate> roots = ModRefCandidateGraph.findMethodRoots(pa, pdg, ignoreExceptions);
				connectParameterStructureRoots(pdg, pdgnode2modref, roots, formals);
			}

			for (final PDGNode call : pdg.getCalls()) {
				final List<PDGNode> actuals = new LinkedList<PDGNode>();
				for (final PDGEdge e : pdg.outgoingEdgesOf(call)) {
					if (e.kind == PDGEdge.Kind.CONTROL_DEP_EXPR
							&& (e.to.getKind() == PDGNode.Kind.ACTUAL_IN || e.to.getKind() == PDGNode.Kind.ACTUAL_OUT)
							&& pdgnode2modref.contains(e.to.getId())) {
						actuals.add(e.to);
					}
				}

				connectParameterStructureNodes(pdg, pdgnode2modref, actuals);
				final List<ModRefRootCandidate> roots =
					ModRefCandidateGraph.findCallRoots(pa, pdg, call, ignoreExceptions);
				connectParameterStructureRoots(pdg, pdgnode2modref, roots, actuals);
			}
		}
	}

	private static void	connectParameterStructureRoots(final PDG pdg,
			final TIntObjectHashMap<ModRefFieldCandidate> pdgnode2modref, final List<ModRefRootCandidate> roots,
			final List<PDGNode> nodes) {
		for (final PDGNode n : nodes) {
			final ModRefFieldCandidate cand = pdgnode2modref.get(n.getId());

			for (final ModRefRootCandidate root : roots) {
				if (root.isPotentialParentOf(cand)) {
					pdg.addEdge(root.getNode(), n, PDGEdge.Kind.PARAM_STRUCT);
				}
			}
		}
	}

	private static void	connectParameterStructureNodes(final PDG pdg,
			final TIntObjectHashMap<ModRefFieldCandidate> pdgnode2modref, final List<PDGNode> nodes) {
		for (final PDGNode from : nodes) {
			final ModRefFieldCandidate fromCand = pdgnode2modref.get(from.getId());

			for (final PDGNode to : nodes) {
				if (from == to) { continue; }
				final ModRefFieldCandidate toCand = pdgnode2modref.get(to.getId());

				if (fromCand.isPotentialParentOf(toCand)) {
					pdg.addEdge(from, to, PDGEdge.Kind.PARAM_STRUCT);
				}
				
				if (toCand == fromCand) {
					assert pdg.outgoingEdgesOf(from).stream().noneMatch( e -> e.kind == PDGEdge.Kind.PARAM_EQUIV);
					assert toCand.isMod() && toCand.isRef();
					pdg.addEdge(from, to, PDGEdge.Kind.PARAM_EQUIV);
				}
			}
		}
	}

	private static void connectFormalAndActualParams(final SDGBuilder sdg,
			final TIntObjectHashMap<ModRefFieldCandidate> pdgnode2modref) {
		for (final PDG pdg : sdg.getAllPDGs()) {
			for (final PDGNode call : pdg.getCalls()) {
				final List<PDGNode> actIn = findChildren(pdg, call, PDGNode.Kind.ACTUAL_IN);
				final List<PDGNode> actOut = findChildren(pdg, call, PDGNode.Kind.ACTUAL_OUT);
				final List<PDGNode> entries = findCalledEntries(pdg, call);

				for (final PDGNode entry : entries) {
					final PDG called = sdg.getPDGforId(entry.getPdgId());
					{
						final List<PDGNode> formIn = findChildren(called, entry, PDGNode.Kind.FORMAL_IN);
						for (final PDGNode fIn : formIn) {
							final ModRefFieldCandidate fCand = pdgnode2modref.get(fIn.getId());
							if (fCand == null) { continue; }

							for (final PDGNode aIn : actIn) {
								final ModRefFieldCandidate aCand = pdgnode2modref.get(aIn.getId());

								if (fCand.equals(aCand)) {
									if (!pdg.containsVertex(fIn)) {
										pdg.addVertex(fIn);
									}

									pdg.addEdge(aIn, fIn, PDGEdge.Kind.PARAMETER_IN);
								}
							}
						}
					}

					{
						final List<PDGNode> formOut = findChildren(called, entry, PDGNode.Kind.FORMAL_OUT);
						for (final PDGNode fOut : formOut) {
							final ModRefFieldCandidate fCand = pdgnode2modref.get(fOut.getId());
							if (fCand == null) { continue; }

							for (final PDGNode aOut : actOut) {
								final ModRefFieldCandidate aCand = pdgnode2modref.get(aOut.getId());

								if (fCand.equals(aCand)) {
									if (!called.containsVertex(aOut)) {
										called.addVertex(aOut);
									}

									called.addEdge(fOut, aOut, PDGEdge.Kind.PARAMETER_OUT);
								}
							}
						}
					}
				}
			}
		}
	}

	private static List<PDGNode> findChildren(final PDG pdg, final PDGNode call, final PDGNode.Kind kind) {
		final List<PDGNode> children = new LinkedList<PDGNode>();

		for (final PDGEdge e : pdg.outgoingEdgesOf(call)) {
			if (e.kind == PDGEdge.Kind.CONTROL_DEP_EXPR && e.to.getKind() == kind) {
				children.add(e.to);
			}
		}

		return children;
	}

	private static List<PDGNode> findCalledEntries(final PDG pdg, final PDGNode call) {
		final List<PDGNode> entries = new LinkedList<PDGNode>();

		for (final PDGEdge e : pdg.outgoingEdgesOf(call)) {
			if (e.kind == PDGEdge.Kind.CALL_STATIC || e.kind == PDGEdge.Kind.CALL_VIRTUAL) {
				entries.add(e.to);
			}
		}

		return entries;
	}

	private static void addDataDepEdgesToPDG(final Map<Node, PDGNode> node2pdg, Map<Node, OrdinalSet<Node>> mayRead,
			final PDG pdg) {
		for (final Entry<Node, PDGNode> e : node2pdg.entrySet()) {
			final OrdinalSet<Node> read = mayRead.get(e.getKey());
			final PDGNode to = e.getValue();
			
			if (read == null) continue;
			for (final Node def : read) {
				final PDGNode from = node2pdg.get(def);
				pdg.addEdge(from, to, PDGEdge.Kind.DATA_HEAP);
			}
		}
	}

	private static synchronized Pair<Map<Node, PDGNode>, Map<UniqueParameterCandidate, Set<Node>>> createPDGNodes(final ModRefControlFlowGraph cfg, final PDG pdg,
			final TIntObjectHashMap<ModRefFieldCandidate> pdgnode2modref) {
		final Map<Node, PDGNode> map = new HashMap<Node, PDGNode>();
		final Collection<Node> toUpdateUniqueParameterCandidate2PDGNode = new LinkedList<>();

		for (final Node n : cfg) {
			switch (n.getKind()) {
			case FORMAL_IN: {
				// create form-in
				final PDGNode fIn = createPDGNode(pdg, n.getCandidate(), pdg.entry, PDGNode.Kind.FORMAL_IN, PDGNode.DEFAULT_NO_LOCAL, PDGNode.DEFAULT_NO_LOCAL);
				map.put(n, fIn);
				pdgnode2modref.put(fIn.getId(), n.getCandidate());

				assert  n.isMod();
				toUpdateUniqueParameterCandidate2PDGNode.add(n);
			} break;
			case FORMAL_OUT: {
				// create form-out
				final PDGNode fOut = createPDGNode(pdg, n.getCandidate(), pdg.entry, PDGNode.Kind.FORMAL_OUT, PDGNode.DEFAULT_NO_LOCAL, PDGNode.DEFAULT_NO_LOCAL);
				map.put(n, fOut);
				pdgnode2modref.put(fOut.getId(), n.getCandidate());
				
				assert !n.isMod();
			} break;
			case ACTUAL_IN: {
				// create actual-in
				final PDGNode aIn = createPDGNode(pdg, n.getCandidate(), n.getNode(), PDGNode.Kind.ACTUAL_IN, PDGNode.DEFAULT_NO_LOCAL, PDGNode.DEFAULT_NO_LOCAL);
				map.put(n, aIn);
				pdgnode2modref.put(aIn.getId(), n.getCandidate());
				
				assert !n.isMod();
			} break;
			case ACTUAL_OUT: {
				// create actual-out
				final PDGNode aOut = createPDGNode(pdg, n.getCandidate(), n.getNode(), PDGNode.Kind.ACTUAL_OUT, PDGNode.DEFAULT_NO_LOCAL, PDGNode.DEFAULT_NO_LOCAL);
				map.put(n, aOut);
				pdgnode2modref.put(aOut.getId(), n.getCandidate());
				
				assert n.isMod();
				toUpdateUniqueParameterCandidate2PDGNode.add(n);
			} break;
			case READ: {
				map.put(n, n.getNode());
				assert !n.isMod();
			} break;
			case WRITE: {
				map.put(n, n.getNode());
				assert  n.isMod();
				toUpdateUniqueParameterCandidate2PDGNode.add(n);
			} break;
			default: // nothing to do here
			}
		}

		 // at least this size
		final Map<UniqueParameterCandidate, Set<Node>> uniqueParameterCandidate2PDGNode = new HashMap<>(toUpdateUniqueParameterCandidate2PDGNode.size());
		for (Node n : toUpdateUniqueParameterCandidate2PDGNode) {
			final ParameterCandidate pc = n.getCandidate().pc;
			final Iterable<UniqueParameterCandidate> uniques = pc.isUnique() ? Collections.singleton((UniqueParameterCandidate) pc) : pc.getUniques();
			for (UniqueParameterCandidate unique : uniques ) {
				uniqueParameterCandidate2PDGNode.compute(unique, (k, nodes) -> {
					if (nodes == null) {
						nodes = new HashSet<>();
					}
					nodes.add(n);
					return nodes;
				});
			}
		}


		addControlFlow(cfg, map, pdg);

		return Pair.pair(map, uniqueParameterCandidate2PDGNode);
	}

	private static void addControlFlow(final ModRefControlFlowGraph cfg, final Map<Node, PDGNode> map, final PDG pdg) {
		final Map<PDGNode, LinkedList<PDGNode>> addBefore = new HashMap<PDGNode, LinkedList<PDGNode>>();
		final Map<PDGNode, LinkedList<PDGNode>> addAfter = new HashMap<PDGNode, LinkedList<PDGNode>>();

		for (final Node n : cfg) {
			switch (n.getKind()) {
			case ACTUAL_IN: {
				LinkedList<PDGNode> before = addBefore.get(n.getNode());
				if (before == null) {
					before = new LinkedList<PDGNode>();
					addBefore.put(n.getNode(), before);
				}
				final PDGNode pdgN = map.get(n);
				before.add(pdgN);
			} break;
			case ACTUAL_OUT: {
				LinkedList<PDGNode> after = addAfter.get(n.getNode());
				if (after == null) {
					after = new LinkedList<PDGNode>();
					addAfter.put(n.getNode(), after);
				}
				final PDGNode pdgN = map.get(n);
				after.add(pdgN);
			} break;
			case FORMAL_IN: {
				LinkedList<PDGNode> after = addAfter.get(pdg.entry);
				if (after == null) {
					after = new LinkedList<PDGNode>();
					addAfter.put(pdg.entry, after);
				}
				final PDGNode pdgN = map.get(n);
				after.add(pdgN);
			} break;
			case FORMAL_OUT: {
				LinkedList<PDGNode> before = addBefore.get(pdg.exit);
				if (before == null) {
					before = new LinkedList<PDGNode>();
					addBefore.put(pdg.exit, before);
				}
				final PDGNode pdgN = map.get(n);
				before.add(pdgN);
			} break;
			default: // nothing to do here
			}
		}

		for (final Entry<PDGNode, LinkedList<PDGNode>> e : addBefore.entrySet()) {
			addNodesBefore(pdg, e.getKey(), e.getValue());
		}

		for (final Entry<PDGNode, LinkedList<PDGNode>> e : addAfter.entrySet()) {
			addNodesAfter(pdg, e.getKey(), e.getValue());
		}
	}

	private static void addNodesBefore(final PDG pdg, final PDGNode n, final LinkedList<PDGNode> before) {
		final List<PDGEdge> incoming = new LinkedList<PDGEdge>();
		for (final PDGEdge e : pdg.incomingEdgesOf(n)) {
			if (e.kind == PDGEdge.Kind.CONTROL_FLOW || e.kind == PDGEdge.Kind.CONTROL_FLOW_EXC) {
				incoming.add(e);
			}
		}
		final PDGNode first = before.getFirst();
		for (PDGEdge in : incoming) {
			pdg.removeEdge(in);
			pdg.addEdge(in.from, first, in.kind);
		}

		PDGNode previous = null;
		for (final PDGNode current : before) {
			if (previous != null) {
				pdg.addEdge(previous, current, PDGEdge.Kind.CONTROL_FLOW);
			}

			previous = current;
		}

		final PDGNode last = before.getLast();
		pdg.addEdge(last, n, PDGEdge.Kind.CONTROL_FLOW);
	}

	private static void addNodesAfter(final PDG pdg, final PDGNode n, final LinkedList<PDGNode> after) {
		final List<PDGEdge> outgoing = new LinkedList<PDGEdge>();
		for (final PDGEdge e : pdg.outgoingEdgesOf(n)) {
			if (e.kind == PDGEdge.Kind.CONTROL_FLOW || e.kind == PDGEdge.Kind.CONTROL_FLOW_EXC) {
				outgoing.add(e);
			}
		}

		final PDGNode last = after.getLast();
		for (PDGEdge out : outgoing) {
			pdg.removeEdge(out);
			pdg.addEdge(last, out.to, out.kind);
		}

		PDGNode previous = null;
		for (final PDGNode current : after) {
			if (previous != null) {
				pdg.addEdge(previous, current, PDGEdge.Kind.CONTROL_FLOW);
			}

			previous = current;
		}

		final PDGNode first = after.getFirst();
		pdg.addEdge(n, first, PDGEdge.Kind.CONTROL_FLOW);
	}

	private static PDGNode createPDGNode(final PDG pdg, final ModRefFieldCandidate c, final PDGNode parent,
			final PDGNode.Kind kind, final String[] localDefNames, final String[] localUseNames) {
		final String label = c.toString();
		final TypeReference type = c.getType();
		final PDGNode newNode = pdg.createNode(label, kind, type, localDefNames, localUseNames);
		newNode.setSourceLocation(parent.getSourceLocation());
		newNode.setBytecodeIndex(c.getBytecodeIndex());
		newNode.setBytecodeName(c.getBytecodeName());
		final OrdinalSet<ParameterField> fields = c.getFields();
		if (fields.size() == 1) {
			newNode.setParameterField(fields.iterator().next());
		} else {
			newNode.setParameterField(null);
		}
		pdg.addEdge(parent, newNode, PDGEdge.Kind.CONTROL_DEP_EXPR);

		return newNode;
	}

	private static Map<Node, OrdinalSet<Node>> computeLastReachingDefsSlow(final BitVectorSolver<Node> solver,
			final OrdinalSetMapping<Node> domain, final ModRefProvider provider,
			boolean isParallel) {
		final Map<Node, OrdinalSet<Node>> mayRead = Collections.synchronizedMap(new HashMap<Node, OrdinalSet<Node>>());

		StreamSupport.stream(domain.spliterator(), isParallel).forEach(n -> {
			final BitVectorVariable in = solver.getIn(n);
			final IntSet inSet = in.getValue();
			
			if (inSet == null) {
				final OrdinalSet<Node> empty = new OrdinalSet<>(EmptyIntSet.instance, domain);
				mayRead.put(n, empty);
			} else {
				final IntSet mayRef = provider.getMayRef(n, inSet, domain);
				final IntSet reachingDefs = mayRef;
				final OrdinalSet<Node> reachDefSet = new OrdinalSet<ModRefControlFlowGraph.Node>(reachingDefs, domain);
				mayRead.put(n, reachDefSet);
			}
		});

		return mayRead;
	}
	
	private static Map<Node, OrdinalSet<Node>> computeLastReachingDefs(
			final Iterable<Node> reads,
			final Map<UniqueParameterCandidate, Set<UniqueParameterCandidate>> mayAliased,
			final Map<UniqueParameterCandidate, Set<Node>> uniqueParameterCandidate2Node,
			final BitVectorSolver<Node> solver,
			final OrdinalSetMapping<Node> domain, final ModRefProvider provider,
			boolean isParallel) {
		final Map<Node, OrdinalSet<Node>> mayRead = Collections.synchronizedMap(new HashMap<Node, OrdinalSet<Node>>());

		StreamSupport.stream(reads.spliterator(), isParallel).forEach(n -> {
			final BitVectorVariable in = solver.getIn(n);
			final IntSet inSet = in.getValue();

			if (inSet == null) {
				final OrdinalSet<Node> empty = new OrdinalSet<ModRefControlFlowGraph.Node>(new EmptyIntSet(), domain);
				mayRead.put(n, empty);
			} else {
				final BitVector bvMayRef = new BitVector(domain.getSize());
				int maxSet = -1;
				
				final ModRefFieldCandidate nCand = n.getCandidate();
				final Iterable<UniqueParameterCandidate> uniques = nCand.pc.isUnique() ? Collections.singleton((UniqueParameterCandidate)nCand.pc) : nCand.pc.getUniques();
				for (UniqueParameterCandidate unique : uniques) {
					final Set<UniqueParameterCandidate> aliased = mayAliased.get(unique);
					if (aliased == null) continue;
					for (UniqueParameterCandidate aliasedUnique : aliased ) {
						final Iterable<Node> otherNodes = uniqueParameterCandidate2Node.get(aliasedUnique);
						if (otherNodes == null) continue;
						for (Node other : otherNodes) {
							final int id = domain.getMappedIndex(other);
							if (inSet.contains(id)) {
								assert 0 <= id && id < domain.getSize();
								bvMayRef.set(id);
								maxSet = Math.max(maxSet, id);
							}
						}
					}
				}
				
				if (maxSet >= 0) {
					// implicitly creates a copy just large enough to hold all necessary bits
					mayRead.put(n, new OrdinalSet<ModRefControlFlowGraph.Node>(new BitVectorIntSet(bvMayRef, maxSet), domain));
				} else {
					mayRead.put(n, new OrdinalSet<ModRefControlFlowGraph.Node>(new EmptyIntSet(),                     domain));
				}
			}
		});

		return mayRead;
	}

}
