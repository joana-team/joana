/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.output;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.slicer.ExceptionalReturnCaller;
import com.ibm.wala.ipa.slicer.GetCaughtExceptionStatement;
import com.ibm.wala.ipa.slicer.HeapStatement;
import com.ibm.wala.ipa.slicer.HeapStatement.HeapParamCaller;
import com.ibm.wala.ipa.slicer.HeapStatement.HeapReturnCaller;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.ParamCallee;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Statement.Kind;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAAbstractThrowInstruction;
import com.ibm.wala.ssa.SSAAbstractUnaryInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

import edu.kit.joana.deprecated.jsdg.SDGFactory;
import edu.kit.joana.deprecated.jsdg.sdg.interference.WalaSDGInterferenceComputation;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.deprecated.jsdg.wala.BytecodeLocation;
import edu.kit.joana.deprecated.jsdg.wala.SourceLocation;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.IKey2Origin;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Operation;
import edu.kit.joana.ifc.sdg.graph.SDGVerifier;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Converts the SDG of the WALA framework to an SDG in the joana format.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class WalaConverter {

	private final SDG wSDG;
	private final boolean addControlFlow;
	private final boolean ignoreExceptions;
	private final String entryMethod;
	private final boolean computeInterference;
	private final boolean optimizeThisAccess;
	private final boolean useEscapeOpt;
	private final IKey2Origin k2o;

	public WalaConverter(final SDG wSDG, final SDGFactory.Config cfg,
			final IKey2Origin k2o) {
		this.wSDG = wSDG;
		this.entryMethod = cfg.mainClass;
		this.addControlFlow = cfg.addControlFlow;
		this.ignoreExceptions = cfg.ignoreExceptions;
		this.computeInterference = cfg.computeInterference;
		this.optimizeThisAccess = cfg.interferenceOptimizeThisAccess;
		this.useEscapeOpt = cfg.interferenceUseEscape;
		this.k2o = k2o;
	}

	private edu.kit.joana.ifc.sdg.graph.SDG sdg;

	private Map<String, Integer> nodes;
	private Map<Integer, SourceLocation> pdgId2sourceLoc;

	private CallGraph cg;
	private CGNode cgRoot;

	private Map<Integer, SDGNode> sdgNodes;

	private MutableIntSet pdgsDone;
	private Map<Integer, SDGNode> id2entry;

	/**
	 * Always start with 1 as joana.SDG expects the root node to have id 1.
	 */
	private int currentId = 1;

	private String getId(final int pdgId, final int id) {
		return pdgId + ":" + id;
	}

	private int id(final int pdgId, final int id) {
		final String str = getId(pdgId, id);
		Integer unique = nodes.get(str);
		if (unique == null) {
			unique = currentId;
			currentId++;
			nodes.put(str, unique);
		}

		return unique;
	}

	private SDGNode createNodeAsNeeded(final int id, final Operation op,
			final String label, final int pdgId, final SourceLocation pos,
			final BytecodeLocation bcLoc) {
		SDGNode node = sdgNodes.get(id);
		if (node == null) {
			//TODO create meaningful type - seems to be unused in joana code.. -> remove?
			final String bcMethod = (bcLoc == null ? null : bcLoc.bcMethod);
			final int bcIndex = (bcLoc == null ? -1 : bcLoc.bcIndex);
			final String type = "";
			if (pos == null) {
				node = new SDGNode(id, op, label, pdgId, type, null, 0, 0, 0, 0, bcMethod, bcIndex);
			} else {
				node = new SDGNode(id, op, label, pdgId, type, pos.getSourceFile(),
						pos.getStartRow(), pos.getStartColumn(),
						pos.getEndRow(), pos.getEndColumn(), bcMethod, bcIndex);
			}

			sdgNodes.put(id, node);
		}

		return node;
	}

	private SDGNode node(final int pdgId, final int id) {
		final int nodeId = id(pdgId, id);
		return sdgNodes.get(nodeId);
	}

	/**
	 * As the WALA SDG uses lazy computation this method does not only
	 * convert the SDG to Joana Format, but it also triggers the computation
	 * of the WALA SDG. This may take long and consume much memory!
	 *
	 * @param progress
	 * @return
	 * @throws CancelException
	 * @throws WalaException
	 */
	public edu.kit.joana.ifc.sdg.graph.SDG convertToJoanaSDG(final IProgressMonitor progress) throws CancelException, WalaException {
		sdg = new edu.kit.joana.ifc.sdg.graph.SDG(entryMethod);
		currentId = 1;
		nodes = new HashMap<String, Integer>();
		pdgId2sourceLoc = new HashMap<Integer, SourceLocation>();
		pdgsDone = IntSetUtil.make();
		id2entry = new HashMap<Integer, SDGNode>();
		sdgNodes = new HashMap<Integer, SDGNode>();

		cg = wSDG.getCallGraph();
		cgRoot = cg.getFakeRootNode();

		progress.beginTask("Converting WALA SDG to Joana SDG", cg.getMaxNumber());

		final int rootId = cg.getNumber(cgRoot);
		convert(cgRoot, rootId, progress);

        if (progress.isCanceled()) {
            throw CancelException.make("Operation aborted.");
        }

		if (addControlFlow) {
			progress.subTask("Adding control flow to SDG");
			fixControlFlow();
			progress.done();
		}

        if (progress.isCanceled()) {
            throw CancelException.make("Operation aborted.");
        }

        if (computeInterference) {
			progress.subTask("Computing thread interferences");
			computeInterference(progress);
			progress.done();
		}

		assert (SDGVerifier.verify(sdg, false, addControlFlow) == 0) : "SDG verification failed.";

		progress.done();

		return sdg;
	}

	private void computeInterference(final IProgressMonitor progress) throws CancelException, WalaException {
		final PointerAnalysis pts = wSDG.getPointerAnalysis();
		final HeapGraph hg = pts.getHeapGraph();

		WalaSDGInterferenceComputation ifc =
			new WalaSDGInterferenceComputation(wSDG, cg, optimizeThisAccess, useEscapeOpt, hg, k2o);

		ifc.compute(progress);

		TIntObjectHashMap<MutableIntSet> readWrite = ifc.getReadWrites();
		for (int fromId : readWrite.keys()) {
			final Statement from = wSDG.getNode(fromId);
			final int fromPdgId = cg.getNumber(from.getNode());
			final PDG fromPdg = wSDG.getPDG(from.getNode());
			final SDGNode fromNode = node(fromPdgId, fromPdg.getNumber(from));

			IntSet toSet = readWrite.get(fromId);
			toSet.foreach(new IntSetAction(){

				public void act(int toId) {
					final Statement to = wSDG.getNode(toId);
					final int toPdgId = cg.getNumber(to.getNode());
					final PDG toPdg = wSDG.getPDG(to.getNode());
					final SDGNode toNode = node(toPdgId, toPdg.getNumber(to));

					addReadWriteInterference(fromNode, toNode);
				}

			});
		}

		TIntObjectHashMap<MutableIntSet> writeWrite = ifc.getWriteWrites();
		for (int fromId : writeWrite.keys()) {
			final Statement from = wSDG.getNode(fromId);
			final int fromPdgId = cg.getNumber(from.getNode());
			final PDG fromPdg = wSDG.getPDG(from.getNode());
			final SDGNode fromNode = node(fromPdgId, fromPdg.getNumber(from));

			IntSet toSet = writeWrite.get(fromId);
			toSet.foreach(new IntSetAction(){

				public void act(int toId) {
					final Statement to = wSDG.getNode(toId);
					final int toPdgId = cg.getNumber(to.getNode());
					final PDG toPdg = wSDG.getPDG(to.getNode());
					final SDGNode toNode = node(toPdgId, toPdg.getNumber(to));

					addWriteWriteInterference(fromNode, toNode);
				}

			});
		}

	}

	/**
	 * Phi nodes and catch instructions are not part of the control flow graph in
	 * wala. (Reason: They do not represent an actual instruction and therefore
	 * are never executed)
	 * Because we want to have an sdg with controlflow for each node, we simply
	 * remove them. We connect all sources of incoming edges to all targets
	 * of their outgoing edges.
	 */
	private void fixControlFlow() {
		// fix broken control flow for phi and catch instructions by removing them
		final Set<SDGNode> toRemove = HashSetFactory.make();

		// add return edges to calls
		for (SDGNode node : sdg.vertexSet()) {
			if (node.kind == edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.CALL) {
				Set<SDGEdge> out = sdg.outgoingEdgesOf(node);
				Set<SDGNode> succs = HashSetFactory.make();
				Set<SDGNode> calls = HashSetFactory.make();
				for (SDGEdge edge : out) {
					if (edge.getKind() == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CALL) {
						// add possible target
						calls.add(edge.getTarget());
					} else if (edge.getKind() == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_FLOW) {
						// add controlflow succs
						succs.add(edge.getTarget());
					}
				}

				for (SDGNode entry : calls) {
					assert (entry.getKind() == edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.ENTRY);

					SDGNode exit = findExit(entry);

					for (SDGNode succ : succs) {
						addReturnFlow(exit, succ);
					}
				}
			}
		}

		for (SDGNode node : sdg.vertexSet()) {
			boolean brokenControlFlow = false;

			if (node.kind != edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.EXIT) {
				boolean hasOutFlow = false;
				for (final SDGEdge edge : sdg.outgoingEdgesOf(node)) {
					if (edge.getKind() == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_FLOW) {
						hasOutFlow = true;
						break;
					}
				}

				brokenControlFlow |= !hasOutFlow;

//				if (!hasOutFlow) {
//					System.err.println("No outflow for " + node.getKind() + ": " + node.getLabel());
//					addControlFlow(node, pdgExit);
//				}
			}

			if (node.kind != edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.ENTRY) {
				boolean hasInFlow = false;
				for (final SDGEdge edge : sdg.incomingEdgesOf(node)) {
					if (edge.getKind() == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_FLOW) {
						hasInFlow = true;
						break;
					}
				}

				brokenControlFlow |= !hasInFlow;

//				if (!hasInFlow) {
//					System.err.println("No inflow for " + node.getKind() + ": " + node.getLabel());
//					addControlFlow(pdgEntry, node);
//				}
			}

			if (brokenControlFlow) {
				// delete node and restructure edges later
				toRemove.add(node);
			}
		}

		for (final SDGNode node : toRemove) {
			removeNode(node);
		}
	}

	private SDGNode findExit(SDGNode entry) {
		SDGNode exit = null;
		// entry must have a direct connection to exit vie control flow
		// we use this property and search exit only in the direct successors of entry
		Set<SDGEdge> out = sdg.outgoingEdgesOf(entry);

		for (SDGEdge edge : out) {
			if (edge.getKind() == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_FLOW
					&& edge.getTarget().getKind() == edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.EXIT) {
				exit = edge.getTarget();
				break;
			}
		}

		assert (exit != null) : "No direct control flow from entry to exit: no exit found!";

		return exit;
	}

	private void removeNode(final SDGNode node) {
		final Set<SDGEdge> incoming = HashSetFactory.make();

		for (final SDGEdge edge : sdg.incomingEdgesOf(node)) {
			if (edge.getSource() != node) {
				incoming.add(edge);
			}
		}

		for (final SDGEdge edge : incoming) {
			sdg.removeEdge(edge);
		}

		final Set<SDGEdge> outgoing = HashSetFactory.make();

		for (final SDGEdge edge : sdg.outgoingEdgesOf(node)) {
			if (edge.getTarget() != node) {
				outgoing.add(edge);
			}
		}

		for (final SDGEdge edge : outgoing) {
			sdg.removeEdge(edge);
		}

		for (final SDGEdge in : incoming) {
			for (final SDGEdge out : outgoing) {
				final SDGNode source = in.getSource();
				final SDGNode target = out.getTarget();
				if (source != target) {
					// do not add self referencing dependencies. they are present implicit
					addEdge(source, target, in.getKind());

					if (in.getKind() != out.getKind()) {
						addEdge(source, target, out.getKind());
					}
				}

			}
		}

		sdg.removeVertex(node);
	}

	/**
	 *
	 * @param method
	 * @return Entry node of the created pdg
	 * @throws CancelException
	 */
	private SDGNode convert(final CGNode method, final int pdgId, final IProgressMonitor progress) throws CancelException {
		final PDG pdg = wSDG.getPDG(method);
		Statement entry = null;
		Statement exit = null;

		pdgsDone.add(pdgId);

		for (final Statement s : pdg) {
			if (s.getKind() == Kind.METHOD_ENTRY) {
				assert (entry == null) : "More then one entry found.";

				entry = s;
			} else if (s.getKind() == Kind.METHOD_EXIT) {
				assert (exit == null) : "More then one exit found.";

				exit = s;
			}

	        if (progress.isCanceled()) {
	            throw CancelException.make("Operation aborted.");
	        }

		}

		if (entry == null) {
			throw new IllegalStateException("No entry in pdg.");
		}
		if (exit == null) {
			throw new IllegalStateException("No exit in pdg.");
		}

		progress.worked(1);
		Log.info("Computing PDG for " + (pdg.getCallGraphNode().getIR() == null ? "(No IR) " : "") + entry);

		return convert(pdg, entry, exit, pdgId, progress);
	}

	private SDGNode createNode(final int pdgId, final int id,
			final Statement next, final IMethod im, final String sourceFileName) {
		SourceLocation loc = null;
		BytecodeLocation bcLoc = null;

		if (next instanceof StatementWithInstructionIndex) {
			final StatementWithInstructionIndex swii = (StatementWithInstructionIndex) next;
			loc = getSourceLocation(im, swii, sourceFileName);
			if (im instanceof ShrikeBTMethod) {
				ShrikeBTMethod shrikeMethod = (ShrikeBTMethod) im;
				int bcIndex = -1;
				try {
					bcIndex = shrikeMethod.getBytecodeIndex(swii.getInstructionIndex());
				} catch (InvalidClassFileException e) {}

				bcLoc = new BytecodeLocation(im.getSignature(), bcIndex);
			} else {
				bcLoc = new BytecodeLocation(im.getSignature(), -1);
			}
		} else {
			switch (next.getKind()) {
			// form-in/out nodes - connect to method entry pos
			case EXC_RET_CALLEE:
			case HEAP_PARAM_CALLEE:
			case HEAP_RET_CALLEE:
			case NORMAL_RET_CALLEE:
			case PARAM_CALLEE:
				loc = pdgId2sourceLoc.get(pdgId);
				break;

			// act-in/out nodes - connects to pos of call instruction
			case EXC_RET_CALLER: {
				// should be a StatementWithInstructionIndex - but we treat it
				// also here just to be sure. ;)
				final ExceptionalReturnCaller erc = (ExceptionalReturnCaller) next;
				final SSAInstruction instr = erc.getInstruction();
				final int index = erc.getInstructionIndex();
				loc = getSourceLocation(im, index, instr, sourceFileName);
			} break;
			case HEAP_PARAM_CALLER: {
				final HeapParamCaller hpc = (HeapParamCaller) next;
				final int callIndex = hpc.getCallIndex();
				final SSAInstruction instr = hpc.getCall();
				loc = getSourceLocation(im, callIndex, instr, sourceFileName);
			} break;
			case HEAP_RET_CALLER: {
				final HeapReturnCaller hrc = (HeapReturnCaller) next;
				final int callIndex = hrc.getCallIndex();
				final SSAInstruction instr = hrc.getCall();
				loc = getSourceLocation(im, callIndex, instr, sourceFileName);
			} break;
			case NORMAL_RET_CALLER: {
				// should be a StatementWithInstructionIndex - but we treat it
				// also here just to be sure. ;)
				final NormalReturnCaller nrc = (NormalReturnCaller) next;
				final SSAInstruction instr = nrc.getInstruction();
				final int index = nrc.getInstructionIndex();
				loc = getSourceLocation(im, index, instr, sourceFileName);
			} break;
			case PARAM_CALLER: {
				// should be a StatementWithInstructionIndex - but we treat it
				// also here just to be sure. ;)
				final ParamCaller pc = (ParamCaller) next;
				final SSAInstruction instr = pc.getInstruction();
				final int index = pc.getInstructionIndex();
				loc = getSourceLocation(im, index, instr, sourceFileName);
			} break;
			default: // nothing to do here
			}
		}

		final SDGNode sdgNode = createNodeAsNeeded(id(pdgId, id), op(next), label(next), pdgId, loc, bcLoc);
		sdg.addVertex(sdgNode);

		return sdgNode;
	}

	/**
	 *
	 * @param pdg
	 * @param entry
	 * @param exit
	 * @return entry node of the created pdg
	 * @throws CancelException
	 */
	private SDGNode convert(final PDG pdg, final Statement entry,
			final Statement exit, final int pdgId, final IProgressMonitor progress) throws CancelException {
		final IMethod imethod = pdg.getCallGraphNode().getMethod();
		final IClass declaringClass = imethod.getDeclaringClass();
		final String sourceFileName = Util.sourceFileName(declaringClass.getName());

		final int idEntry = pdg.getNumber(entry);
		final String methodName = (pdg.getCallGraphNode() == cgRoot
				? "*Start*" : Util.methodName(imethod));

		final boolean isThreadStart = isThreadStart(imethod);

		SourceLocation locEntryExit = getSourceLocation(imethod, 0, null, sourceFileName);
		if (locEntryExit != null) {
			// hack: simply assume that the method header will be 1 line above
			// the line of the first instruction.
			final int sr = locEntryExit.getStartRow();
			final int er = locEntryExit.getEndRow();
			locEntryExit = SourceLocation.getLocation(sourceFileName,
					(sr == 0 ? 0 : sr - 1), 0, (er == 0 ? 0 : er - 1), 0);
		}
		pdgId2sourceLoc.put(pdgId, locEntryExit);

		BytecodeLocation bcLocEntryExit = new BytecodeLocation(imethod.getSignature(), -1);

		final SDGNode pdgEntry = createNodeAsNeeded(id(pdgId, idEntry),
				Operation.ENTRY, methodName, pdgId, locEntryExit, bcLocEntryExit);

		// set class loader info
		IMethod im = pdg.getCallGraphNode().getMethod();
		if (im != null) {
			IClass cls = im.getDeclaringClass();

			if (cls != null) {
				final String clsLoader = cls.getClassLoader().toString();
				pdgEntry.setClassLoader(clsLoader);
			}
		}

		sdg.addVertex(pdgEntry);
		id2entry.put(pdgId, pdgEntry);

		final int idExit = pdg.getNumber(exit);
		final SDGNode pdgExit = createNodeAsNeeded(id(pdgId, idExit),
				Operation.EXIT, methodName, pdgId, locEntryExit, bcLocEntryExit);
		sdg.addVertex(pdgExit);
		addControlDepExpr(pdgEntry, pdgExit);

		for (Statement s : pdg) {
	        if (progress.isCanceled()) {
	            throw CancelException.make("Operation aborted.");
	        }

			final int sId = pdg.getNumber(s);

			final SDGNode sdgNode = createNode(pdgId, sId, s, imethod, sourceFileName);

			final Iterator<? extends Statement> succs = pdg.getSuccNodes(s);
			while (succs.hasNext()) {
				final Statement next = succs.next();
				final int nextId = pdg.getNumber(next);

				final SDGNode nextNode =
					createNode(pdgId, nextId, next, imethod, sourceFileName);

				if (sdgNode != nextNode) {
					// self recursive deps are omitted - we do not need them as they are always present implicit
					if (pdg.isControlDependend(s, next)) {
						addControlDepUncond(sdgNode, nextNode);
					} else {
						addDataDep(sdgNode, nextNode);
					}
				}
			}

			if (s.getKind() == Kind.NORMAL) {
				final NormalStatement ns = (NormalStatement) s;
				final SSAInstruction instr = ns.getInstruction();
				if (instr != null && instr instanceof SSAReturnInstruction) {
					addDataDep(sdgNode, pdgExit);
				}
			} else if (s.getKind() == Kind.EXC_RET_CALLEE || s.getKind() == Kind.NORMAL_RET_CALLEE) {
				addDataDep(sdgNode, pdgExit);
			}

		}

		// Connect act-in act-outs with callsite invoke
		// And create other pdgs
		for (final Statement stat : pdg) {
	        if (progress.isCanceled()) {
	            throw CancelException.make("Operation aborted.");
	        }

			if (stat.getKind() == Kind.NORMAL) {
				final NormalStatement ns = (NormalStatement) stat;

				final SSAInstruction instr = ns.getInstruction();
				if (instr instanceof SSAAbstractInvokeInstruction) {
					final int nodeId = pdg.getNumber(stat);
					final SDGNode node = node(pdgId, nodeId);
					if (node == null) {
						Log.warn("no node for " + instr);
						continue;
					}

					final SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) instr;
					final CallSiteReference cref = call.getCallSite();
					final Set<CGNode> targets = cg.getPossibleTargets(pdg.getCallGraphNode(), cref);

					// connect act-in/outs with call node
					final Set<Statement> actInStats = pdg.getCallerParamStatements(call);
					for (Statement actInStat : actInStats) {
						final int id = pdg.getNumber(actInStat);
						final SDGNode aIn = node(pdgId, id);
						addControlDepExpr(node, aIn);
					}

					final Set<Statement> actOutStats = pdg.getCallerReturnStatements(call);
					for (Statement actOutStat : actOutStats) {
						final int id = pdg.getNumber(actOutStat);
						final SDGNode aOut = node(pdgId, id);
						addControlDepExpr(node, aOut);
					}

					// Connect callsite to pdg entries & create new pdgs as needed
					for (CGNode target : targets) {
						final int targetId = cg.getNumber(target);
						final SDGNode targetEntry;

						if (!pdgsDone.contains(targetId)) {
							targetEntry = convert(target, targetId, progress);
						} else {
							targetEntry = id2entry.get(targetId);

							assert (targetEntry != null);
							assert (node != null);
						}

						assert (targetEntry.getKind() == edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.ENTRY);

						final boolean forkEdges;
						if (computeInterference && isThreadStart && target.getMethod().getSignature().contains(".run()V")) {
							// fork whenever thread.start calls a .run()V method
							addForkEdge(node, targetEntry);
							forkEdges = true;
						} else {
							addCallDep(node, targetEntry);
							forkEdges = false;
						}

						final PDG callee = wSDG.getPDG(target);
						final Statement[] params = callee.getParamCalleeStatements();
						connect(pdg, pdgId, actInStats, actOutStats, callee,
								targetId, params, forkEdges);
						final Statement[] formOuts = callee.getReturnStatements();
						connect(pdg, pdgId, actInStats, actOutStats, callee,
								targetId, formOuts, forkEdges);

						if (target.getMethod().getReturnType() != TypeReference.Void) {
							Statement calleeExit = null;
							for (Statement st : callee) {
								if (st.getKind() == Kind.METHOD_EXIT) {
									calleeExit = st;
									break;
								}
							}
							SDGNode sdgExit = node(targetId, callee.getNumber(calleeExit));

							for (Statement aOut : actOutStats) {
								if (aOut.getKind() == Kind.NORMAL_RET_CALLER) {
									SDGNode aOutCaller = node(pdgId, pdg.getNumber(aOut));
									addEdge(sdgExit, aOutCaller, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.PARAMETER_OUT);
								}
							}
						}
					}

					// when no call is found - conservatively approximate that all ins may influence all outs
					if (targets.isEmpty()) {
						Log.info("Adding stub for unresolved call in " + methodName
								+ " to " + Util.methodName(call.getDeclaredTarget()));

						for (Statement aInStat : actInStats) {
							SDGNode aIn = node(pdgId, pdg.getNumber(aInStat));
							for (Statement aOutStat : actOutStats) {
								SDGNode aOut = node(pdgId, pdg.getNumber(aOutStat));
								addEdge(aIn, aOut, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.SUMMARY);
							}
						}
					}
				}
			}
		}

		/*
		 * Add Controldependency to root node if none is present.
		 */
		for (final Statement stat : pdg) {
	        if (progress.isCanceled()) {
	            throw CancelException.make("Operation aborted.");
	        }

			if (stat.getKind() == Kind.METHOD_ENTRY) {
				// entry node needs no controldependence
				continue;
			}

			final int nodeId = pdg.getNumber(stat);
			final SDGNode node = node(pdgId, nodeId);
			if (node == null) {
				continue;
			}

			boolean hasControlDep = false;
			for (SDGEdge in : sdg.incomingEdgesOf(node)) {
				switch (in.getKind()) {
				case CONTROL_DEP_CALL:
				case CONTROL_DEP_COND:
				case CONTROL_DEP_UNCOND:
				case CONTROL_DEP_EXPR:
					hasControlDep = true;
					break;
				default: // nothing to do here
				}

				if (hasControlDep) {
					break;
				}
			}
			if (!hasControlDep) {
				switch (node.getKind()) {
				case FORMAL_IN:
				case FORMAL_OUT:
				case EXIT:
					addControlDepExpr(pdgEntry, node);
					break;
				default:
					addControlDepUncond(pdgEntry, node);
				}
			}
		}

		if (addControlFlow) {
			// hack as wala does not include control flow from entry to exit
			addControlFlow(pdgEntry, pdgExit);

			final IR ir = pdg.getCallGraphNode().getIR();
			if (ir == null || ir.isEmptyIR()) {
				// simply add all nodes in a row between entry and exit
				// start with form-in && end with form-out

				SDGNode step = pdgEntry;

				for (Statement formInStat : pdg.getParamCalleeStatements()) {
					switch (formInStat.getKind()) {
					case PARAM_CALLEE:
					case HEAP_PARAM_CALLEE:
						//find node for param && add controlflow stepEntry -> node
						final int formInId = pdg.getNumber(formInStat);
						final SDGNode formIn = node(pdgId, formInId);
						addControlFlow(step, formIn);
						step = formIn;
						break;
					default: // nothing to do here
					}
				}

				for (Statement stat : pdg) {
					switch (stat.getKind()) {
					case EXC_RET_CALLEE:
					case HEAP_PARAM_CALLEE:
					case HEAP_RET_CALLEE:
					case NORMAL_RET_CALLEE:
					case PARAM_CALLEE:
					case METHOD_ENTRY:
					case METHOD_EXIT:
						// these nodes have already been handeled
						break;
					default:
						final int id = pdg.getNumber(stat);
						final SDGNode node = node(pdgId, id);
						addControlFlow(step, node);
						step = node;
						break;
					}
				}

				for (Statement param : pdg.getReturnStatements()) {
					switch (param.getKind()) {
					case EXC_RET_CALLEE:
					case HEAP_RET_CALLEE:
					case NORMAL_RET_CALLEE:
						// find node for param && add controlflow node -> stepExit
						final int formOutId = pdg.getNumber(param);
						final SDGNode formOut = node(pdgId, formOutId);
						addControlFlow(step, formOut);
						step = formOut;
						break;
					default: // nothing to do here
					}
				}

				addControlFlow(step, pdgExit);
			} else {
				final ExplodedControlFlowGraph ecfg = ExplodedControlFlowGraph.make(ir);

				final Map<IExplodedBasicBlock, Set<SDGNode>> first = HashMapFactory.make();
				final Map<IExplodedBasicBlock, Set<SDGNode>> last = HashMapFactory.make();

				for (final IExplodedBasicBlock prev : ecfg) {
			        if (progress.isCanceled()) {
			            throw CancelException.make("Operation aborted.");
			        }

					createCFGforNode(pdg, pdgId, ecfg, first, last, pdgEntry, pdgExit, prev);

					boolean hasSuccs = false;
					for (final IExplodedBasicBlock block : ecfg.getNormalSuccessors(prev)) {
						createCFGforNode(pdg, pdgId, ecfg, first, last, pdgEntry, pdgExit, block);

						final Set<SDGNode> fromNodes = last.get(prev);
						final Set<SDGNode> toNodes = first.get(block);

						for (final SDGNode from : fromNodes) {
							for (final SDGNode to : toNodes) {
								if (from != to) {
									hasSuccs = true;
									addControlFlow(from, to);
								}
							}
						}

						// get SDG node for instruction
						// - prepend act-ins and append act-out iff it is a call
						// - prepend form-out iff it is exit node
						// - use first form-out for exit/ last form-in for entry
						// - same for act-in /act-out on calls
					}

					if (!ignoreExceptions) {
						for (final IExplodedBasicBlock block : ecfg.getExceptionalSuccessors(prev)) {
					        if (progress.isCanceled()) {
					            throw CancelException.make("Operation aborted.");
					        }

							// do the same as above...
							createCFGforNode(pdg, pdgId, ecfg, first, last, pdgEntry, pdgExit, block);

							final Set<SDGNode> fromNodes = last.get(prev);
							final Set<SDGNode> toNodes = first.get(block);

							for (final SDGNode from : fromNodes) {
								for (final SDGNode to : toNodes) {
									if (from != to) {
										hasSuccs = true;
										addControlFlow(from, to);
									}
								}
							}
						}
					}

					if (!hasSuccs && prev != ecfg.exit()) {
						final SSAInstruction instr = prev.getInstruction();
						if (instr instanceof SSAAbstractThrowInstruction) {
							final Set<SDGNode> fromNodes = last.get(prev);
							//TODO use exception exit
							final Set<SDGNode> toNodes = first.get(ecfg.exit());

							for (final SDGNode from : fromNodes) {
								for (final SDGNode to : toNodes) {
									hasSuccs = true;
									addControlFlow(from, to);
								}
							}
						} else {
							Log.warn("No succs and no instruction: " + prev);
						}
					}
				}
			}
		}

		assert (pdgEntry != null);

		return pdgEntry;
	}

	private static boolean isThreadStart(final IMethod method) {
		return WalaSDGInterferenceComputation.isThreadStart(method);
	}

	private static <T, U> void addToMapSet(Map<T, Set<U>> map, T key, U value) {
		assert (value != null);

		Set<U> set = map.get(key);
		if (set == null) {
			set = HashSetFactory.make();
			map.put(key, set);
		}
		set.add(value);
	}

	private void createCFGforNode(final PDG pdg, final int pdgId, final ExplodedControlFlowGraph ecfg,
			final Map<IExplodedBasicBlock,Set<SDGNode>> first, final Map<IExplodedBasicBlock, Set<SDGNode>> last,
			final SDGNode pdgEntry, final SDGNode pdgExit, final IExplodedBasicBlock block) {

		if (first.containsKey(block)) {
			return;
		}

		final SSAInstruction instr = block.getInstruction();

		if (block.isEntryBlock()) {
			SDGNode stepEntry = pdgEntry;

			for (Statement param : pdg.getParamCalleeStatements()) {
				switch (param.getKind()) {
				case PARAM_CALLEE:
				case HEAP_PARAM_CALLEE:
					//find node for param && add controlflow stepEntry -> node
					final int formInId = pdg.getNumber(param);
					final SDGNode formIn = node(pdgId, formInId);
					addControlFlow(stepEntry, formIn);
					stepEntry = formIn;
					break;
				default: // nothing to do here
				}
			}

			addToMapSet(first, block, pdgEntry);
			addToMapSet(last, block, stepEntry);
		} else if (block.isExitBlock()) {
			SDGNode stepExit = null;

			for (Statement param : pdg.getReturnStatements()) {
				switch (param.getKind()) {
				case EXC_RET_CALLEE:
				case HEAP_RET_CALLEE:
				case NORMAL_RET_CALLEE:
					// find node for param && add controlflow node -> stepExit
					final int formOutId = pdg.getNumber(param);
					final SDGNode formOut = node(pdgId, formOutId);
					if (stepExit != null) {
						addControlFlow(formOut, stepExit);
					} else {
						addControlFlow(formOut, pdgExit);
					}
					stepExit = formOut;
					break;
				default: // nothing to do here
				}
			}

			addToMapSet(first, block, (stepExit == null ? pdgExit : stepExit));
			addToMapSet(last, block, pdgExit);
		} else if (instr instanceof SSAAbstractInvokeInstruction) {
			final SSAAbstractInvokeInstruction call =
				(SSAAbstractInvokeInstruction) instr;

			SDGNode step = null;
			for (final Statement actInStm : pdg.getCallerParamStatements(call)) {
				final int actInId = pdg.getNumber(actInStm);
				final SDGNode actIn = node(pdgId, actInId);
				if (step != null) {
					addControlFlow(step, actIn);
				} else {
					addToMapSet(first, block, actIn);
				}
				step = actIn;
			}

			final Statement callStm =
				findStatementForInstruction(pdg, instr);

			if (callStm != null) {
				final int callId = pdg.getNumber(callStm);
				final SDGNode callNode = node(pdgId, callId);

				if (step != null) {
					addControlFlow(step, callNode);
				} else {
					addToMapSet(first, block, callNode);
				}

				step = callNode;
			} else {
				Log.warn("No Statement found for call: " + instr);
			}

			for (final Statement actOutStm : pdg.getCallerReturnStatements(call)) {
				final int actOutId = pdg.getNumber(actOutStm);
				final SDGNode actOut = node(pdgId, actOutId);
				if (step != null) {
					addControlFlow(step, actOut);
				} else {
					addToMapSet(first, block, actOut);
				}
				step = actOut;
			}

			addToMapSet(last, block, step);
		} else if (instr != null) {
			final Statement stm = findStatementForInstruction(pdg, instr);

			if (stm == null) {
				// add last nodes of all predecessors as new last nodes
				// add first nodes of all successors as new first nodes

				final Set<IExplodedBasicBlock> succs =
					searchSuccessorsWithInstruction(ecfg, first, IntSetUtil.make(), block);

				for (final IExplodedBasicBlock succ : succs) {
					createCFGforNode(pdg, pdgId, ecfg, first, last, pdgEntry, pdgExit, succ);

					for (final SDGNode firstNode : first.get(succ)) {
						addToMapSet(first, block, firstNode);
					}
				}

				if (succs.isEmpty()) {
					addToMapSet(first, block, pdgExit);
				}

				final Set<IExplodedBasicBlock> preds =
					searchPredecessorsWithInstruction(ecfg, first, IntSetUtil.make(), block);

				for (final IExplodedBasicBlock pred : preds) {
					createCFGforNode(pdg, pdgId, ecfg, first, last, pdgEntry, pdgExit, pred);

					for (final SDGNode lastNode : last.get(pred)) {
						addToMapSet(last, block, lastNode);
					}
				}

				if (preds.isEmpty()) {
					addToMapSet(last, block, pdgEntry);
				}
			} else {
				final int stmId = pdg.getNumber(stm);
				final SDGNode sdgNode = node(pdgId, stmId);

				addToMapSet(first, block, sdgNode);
				addToMapSet(last, block, sdgNode);
			}
		} else /* instr == null */ {
			// search for successors until instructions are found
			final Set<IExplodedBasicBlock> succs =
				searchSuccessorsWithInstruction(ecfg, first, IntSetUtil.make(), block);

			for (final IExplodedBasicBlock succ : succs) {
				createCFGforNode(pdg, pdgId, ecfg, first, last, pdgEntry, pdgExit, succ);

				for (final SDGNode firstNode : first.get(succ)) {
					addToMapSet(first, block, firstNode);
				}
			}

			if (succs.isEmpty()) {
				addToMapSet(first, block, pdgExit);
			}

			final Set<IExplodedBasicBlock> preds =
				searchPredecessorsWithInstruction(ecfg, first, IntSetUtil.make(), block);

			for (final IExplodedBasicBlock pred : preds) {
				createCFGforNode(pdg, pdgId, ecfg, first, last, pdgEntry, pdgExit, pred);

				for (final SDGNode lastNode : last.get(pred)) {
					addToMapSet(last, block, lastNode);
				}
			}

			if (preds.isEmpty()) {
				addToMapSet(last, block, pdgEntry);
			}
		}
	}

	private Set<IExplodedBasicBlock> searchSuccessorsWithInstruction(
			final ExplodedControlFlowGraph ecfg, final Map<IExplodedBasicBlock, Set<SDGNode>> first,
			final MutableIntSet visited, final IExplodedBasicBlock block) {
		final Set<IExplodedBasicBlock> succs = HashSetFactory.make();

		for (final IExplodedBasicBlock succ : ecfg.getNormalSuccessors(block)) {
			final int id = ecfg.getNumber(succ);
			if (!visited.contains(id)) {
				visited.add(id);

				if (succ.getInstruction() == null) {
					final Set<SDGNode> firstSet = first.get(succ);
					if (firstSet != null && !firstSet.isEmpty()) {
						succs.add(succ);
					} else {
						assert (!succ.isCatchBlock() || succ.getCatchInstruction() != null)
							: "In normal flow: Catch != null: " + succ;

						succs.addAll(searchSuccessorsWithInstruction(ecfg, first, visited, succ));
					}
				} else {
					succs.add(succ);
				}
			}
		}

		if (!ignoreExceptions) {
			for (final IExplodedBasicBlock succ : ecfg.getExceptionalSuccessors(block)) {
				final int id = ecfg.getNumber(succ);
				if (!visited.contains(id)) {
					visited.add(id);

					if (succ.getInstruction() == null) {
						final Set<SDGNode> firstSet = first.get(succ);
						if (firstSet != null && !firstSet.isEmpty()) {
							succs.add(succ);
						} else {
							assert (!succ.isCatchBlock() || succ.getCatchInstruction() != null)
								: "In exc flow: Catch != null: " + succ;

							succs.addAll(searchSuccessorsWithInstruction(ecfg, first, visited, succ));
						}
					} else {
						succs.add(succ);
					}
				}
			}
		}

		return succs;
	}

	private Set<IExplodedBasicBlock> searchPredecessorsWithInstruction(
			final ExplodedControlFlowGraph ecfg, final Map<IExplodedBasicBlock, Set<SDGNode>> first,
			final MutableIntSet visited, final IExplodedBasicBlock block) {
		final Set<IExplodedBasicBlock> succs = HashSetFactory.make();

		for (final IExplodedBasicBlock succ : ecfg.getNormalPredecessors(block)) {
			final int id = ecfg.getNumber(succ);
			if (!visited.contains(id)) {
				visited.add(id);

				if (succ.getInstruction() == null) {
					final Set<SDGNode> firstSet = first.get(succ);
					if (firstSet != null && !firstSet.isEmpty()) {
						succs.add(succ);
					} else {
						assert (!succ.isCatchBlock() || succ.getCatchInstruction() != null)
							: "In normal flow: Catch != null: " + succ;

						succs.addAll(searchSuccessorsWithInstruction(ecfg, first, visited, succ));
					}
				} else {
					succs.add(succ);
				}
			}
		}

		if (!ignoreExceptions) {
			for (final IExplodedBasicBlock succ : ecfg.getExceptionalPredecessors(block)) {
				final int id = ecfg.getNumber(succ);
				if (!visited.contains(id)) {
					visited.add(id);

					if (succ.getInstruction() == null) {
						final Set<SDGNode> firstSet = first.get(succ);
						if (firstSet != null && !firstSet.isEmpty()) {
							succs.add(succ);
						} else {
							assert (!succ.isCatchBlock() || succ.getCatchInstruction() != null)
								: "In exc flow: Catch != null: " + succ;

							succs.addAll(searchSuccessorsWithInstruction(ecfg, first, visited, succ));
						}
					} else {
						succs.add(succ);
					}
				}
			}
		}

		return succs;
	}

	private Statement findStatementForInstruction(final PDG pdg,
			final SSAInstruction instr) {
		Statement matchingStm = null;
		for (final Statement st : pdg) {
			SSAInstruction cmp = null;

			switch (st.getKind()) {
			case CATCH: {
				final GetCaughtExceptionStatement gces =
					(GetCaughtExceptionStatement) st;
				cmp = gces.getInstruction();
			} break;
			case NORMAL: {
				final NormalStatement ns = (NormalStatement) st;
				cmp = ns.getInstruction();
			} break;
			default: // nothing to do here
			}

			if (cmp != null && cmp.equals(instr)) {
				matchingStm = st;
				break;
			}
		}

		return matchingStm;
	}

	private void connect(final PDG pdg, final int pdgId, final Set<Statement> actInStats,
			final Set<Statement> actOutStats, final PDG callee, final int targetId,
			final Statement[] params, final boolean forkEdges) {
		final edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind paramIn =
			(forkEdges ? edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.FORK_IN : edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.PARAMETER_IN);
		final edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind paramOut =
			(forkEdges ? edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.FORK_OUT : edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.PARAMETER_OUT);


		for (final Statement form : params) {
			final int formId = callee.getNumber(form);
			final SDGNode formNode = node(targetId, formId);

			assert (formNode != null);

			switch (form.getKind()) {
			case EXC_RET_CALLEE: {
				boolean paramFound = false;

				for (final Statement aOut : actOutStats) {
					if (aOut.getKind() == Kind.EXC_RET_CALLER) {
						// there should always only be one exc ret callee
						final int actId = pdg.getNumber(aOut);
						final SDGNode actNode = node(pdgId, actId);

						paramFound = true;
						addEdge(formNode, actNode, paramOut);
					}
				}

				if (!paramFound) {
					Log.warn("No actual-out found for: " + form);
				}

			}	break;
			case NORMAL_RET_CALLEE: {
				boolean paramFound = false;

				for (final Statement aOut : actOutStats) {
					if (aOut.getKind() == Kind.NORMAL_RET_CALLER) {
						// there should always only be one normal ret callee
						final int actId = pdg.getNumber(aOut);
						final SDGNode actNode = node(pdgId, actId);

						paramFound = true;
						addEdge(formNode, actNode, paramOut);
					}
				}


				if (!paramFound) {
					Log.warn("No actual-out found for: " + form);
				}

			}	break;
			case HEAP_RET_CALLEE: {
				final HeapStatement heapForm = (HeapStatement) form;
				boolean paramFound = false;

				for (final Statement aOut : actOutStats) {
					if (aOut.getKind() == Kind.HEAP_RET_CALLER) {
						final HeapStatement heapAct = (HeapStatement) aOut;
						if (heapForm.getLocation().equals(heapAct.getLocation())) {
							final int actId = pdg.getNumber(aOut);
							final SDGNode actNode = node(pdgId, actId);

							paramFound = true;
							addEdge(formNode, actNode, paramOut);
						}
					}
				}

				if (!paramFound) {
					Log.warn("No actual-out found for: " + heapForm);
				}

			}	break;
			case HEAP_PARAM_CALLEE: {
				final HeapStatement heapForm = (HeapStatement) form;
				boolean paramFound = false;
				for (final Statement aIn : actInStats) {
					if (aIn.getKind() == Kind.HEAP_PARAM_CALLER) {
						final HeapStatement heapAct = (HeapStatement) aIn;
						if (heapForm.getLocation().equals(heapAct.getLocation())) {
							final int actId = pdg.getNumber(aIn);
							final SDGNode actNode = node(pdgId, actId);

							paramFound = true;
							addEdge(actNode, formNode, paramIn);
						}
					}
				}

				if (!paramFound) {
					Log.warn("No actual-in found for: " + heapForm);
				}

			}	break;
			case PARAM_CALLEE: {
				final ParamCallee fInParam = (ParamCallee) form;
				boolean paramFound = false;
				for (final Statement aIn : actInStats) {
					if (aIn.getKind() == Kind.PARAM_CALLER) {
						final ParamCaller aInParam = (ParamCaller) aIn;
						int paramNum = 0;
						SSAAbstractInvokeInstruction invk = aInParam.getInstruction();
						for (paramNum = 0; paramNum < invk.getNumberOfParameters(); paramNum++) {
							if (invk.getUse(paramNum) == aInParam.getValueNumber()) {
								// value number starts counting at 1; param num at 0;
								if (paramNum + 1 == fInParam.getValueNumber()) {
									final int actId = pdg.getNumber(aIn);
									final SDGNode actNode = node(pdgId, actId);

									addEdge(actNode, formNode, paramIn);
									paramFound = true;
								}
							}
						}
					}
				}

				if (!paramFound) {
					Log.warn("No actual-in found for: " + fInParam);
				}
			}	break;
			default:
				throw new IllegalStateException("Callee param is of kind: " + form.getKind());
			}
		}
	}

	private Operation op(final Statement next) {
		Operation nodeType = Operation.COMPOUND;
		switch (next.getKind()) {
		case EXC_RET_CALLER:
		case HEAP_RET_CALLER:
		case NORMAL_RET_CALLER:
			nodeType = Operation.ACTUAL_OUT;
			break;
		case HEAP_PARAM_CALLER:
		case PARAM_CALLER:
			nodeType = Operation.ACTUAL_IN;
			break;
		case EXC_RET_CALLEE:
		case NORMAL_RET_CALLEE:
		case HEAP_RET_CALLEE:
			nodeType = Operation.FORMAL_OUT;
			break;
		case HEAP_PARAM_CALLEE:
		case PARAM_CALLEE:
			nodeType = Operation.FORMAL_IN;
			break;
		case METHOD_ENTRY:
			nodeType = Operation.ENTRY;
			break;
		case METHOD_EXIT:
			nodeType = Operation.EXIT;
			break;
		case NORMAL:
			final NormalStatement ns = (NormalStatement) next;
			final SSAInstruction instr = ns.getInstruction();
			if (instr instanceof SSAAbstractInvokeInstruction) {
				nodeType = Operation.CALL;
			} else if (instr instanceof SSAMonitorInstruction) {
				nodeType = Operation.MONITOR;
			} else if (instr instanceof SSAConditionalBranchInstruction) {
				nodeType = Operation.IF;
			} else if (instr instanceof SSAAbstractUnaryInstruction) {
				nodeType = Operation.UNARY;
			} else if (instr instanceof SSAArrayLoadInstruction || instr instanceof SSAGetInstruction) {
				nodeType = Operation.REFERENCE;
			} else if (instr instanceof SSAArrayStoreInstruction || instr instanceof SSAPutInstruction) {
				nodeType = Operation.MODIFY;
			} else if (instr.hasDef()) {
				nodeType = Operation.ASSIGN;
			} else {
				nodeType = Operation.COMPOUND;
			}
			break;
		case CATCH:
		case PHI:
		case PI:
			nodeType = Operation.ASSIGN;
			break;
		}

		return nodeType;
	}

	private String label(final Statement next) {
		switch (next.getKind()) {
		case CATCH:
			return "catch";
		case EXC_RET_CALLEE:
			return "form-out (exc)";
		case EXC_RET_CALLER:
			return "act-out (exc)";
		case HEAP_PARAM_CALLEE:
			return "form-in (heap)";
		case HEAP_PARAM_CALLER:
			return "act-in (heap)";
		case HEAP_RET_CALLEE:
			return "form-out (heap)";
		case HEAP_RET_CALLER:
			return "act-out (heap)";
		case METHOD_ENTRY:
			return "entry";
		case METHOD_EXIT:
			return "exit";
		case NORMAL: {
			final NormalStatement ns = (NormalStatement) next;
			final SSAInstruction instr = ns.getInstruction();
			if (instr != null) {
				return Util.prettyShortInstruction(instr);
			} else {
				return "<nop>";
			}
		}
		case NORMAL_RET_CALLEE:
			return "form-out (ret)";
		case NORMAL_RET_CALLER:
			return "act-out (ret)";
		case PARAM_CALLEE: {
			ParamCallee pc = (ParamCallee) next;
			CGNode cgNode = pc.getNode();
			final int valueNr = pc.getValueNumber();
			final boolean isStatic = cgNode.getMethod().isStatic();
			if (!isStatic && valueNr == 1) {
				return "form-in (this)";
			} else {
				return "form-in (param " + (isStatic ? valueNr : valueNr - 1) + ")";
			}
		}
		case PARAM_CALLER: {
			ParamCaller pc = (ParamCaller) next;
			SSAAbstractInvokeInstruction invk = pc.getInstruction();
			final int valueNr = pc.getValueNumber();
			final boolean isStatic = invk.getCallSite().isStatic();
			if (!isStatic && invk.getUse(0) == valueNr) {
				// method is not static and value number == this pointer (parameter no. 0)
				return "this";
			} else {
				for (int paramNum = 0; paramNum < invk.getNumberOfParameters(); paramNum++) {
					if (invk.getUse(paramNum) == valueNr) {
						return "act-in (param " + (isStatic ? paramNum + 1 : paramNum) + ")";
					}
				}

				return "act-in (param ?)";
			}
		}
		case PHI:
			return "phi";
		case PI:
			return "pi";
		}

		throw new IllegalStateException("OOOPS");
	}

	private boolean addEdge(final SDGNode from, final SDGNode to,
			final edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind kind) {
		final SDGEdge edge = new SDGEdge(from, to, kind);

		assert (!(from == to && kind != edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.INTERFERENCE	&& kind != edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.INTERFERENCE_WRITE))
			: "Added self refering dependency that is not an inteference dep: " + from.getId() + "(" + from.getLabel() + ") "
				+ kind + " " + to.getId() + "(" + to.getLabel() + ")";

		return sdg.addEdge(edge);
	}

	private boolean addReadWriteInterference(final SDGNode from, final SDGNode to) {
		return addEdge(from, to, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.INTERFERENCE);
	}

	private boolean addWriteWriteInterference(final SDGNode from, final SDGNode to) {
		return addEdge(from, to, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.INTERFERENCE_WRITE);
	}

	private boolean addControlDepUncond(final SDGNode from, final SDGNode to) {
		final boolean result = addEdge(from, to, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_DEP_UNCOND);
		addEdge(from, to, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.HELP);
		return result;
	}

//TODO use more precise control dependencies
//	private boolean addControlDepCall(final SDGNode from, final SDGNode to) {
//		boolean result = addEdge(from, to, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_DEP_CALL);
//		return result;
//	}
//
//	private boolean addControlDepCond(final SDGNode from, final SDGNode to) {
//		boolean result = addEdge(from, to, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_DEP_COND);
//		addEdge(from, to, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.HELP);
//		return result;
//	}
//
	private boolean addControlDepExpr(SDGNode from, SDGNode to) {
		boolean result = addEdge(from, to, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_DEP_EXPR);
		addEdge(from, to, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.HELP);
		return result;
	}

	private boolean addControlFlow(final SDGNode from, final SDGNode to) {
		if (from.getKind() == edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.EXIT && to.getKind() == edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.ENTRY) {
			throw new IllegalArgumentException("No controlflow from exit to entry allowed. This makes no sense.");
		}

		return addEdge(from, to, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_FLOW);
	}

	private boolean addReturnFlow(final SDGNode from, final SDGNode to) {
		return addEdge(from, to, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.RETURN);
	}

	private boolean addDataDep(final SDGNode from, final SDGNode to) {
		return addEdge(from, to, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.DATA_DEP);
	}

	private boolean addCallDep(final SDGNode from, final SDGNode to) {
		return addEdge(from, to, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CALL);
	}

	private boolean addForkEdge(final SDGNode from, final SDGNode to) {
		return addEdge(from, to, edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.FORK);
	}

	private SourceLocation getSourceLocation(final IMethod im, final int instrIndex,
			final SSAInstruction instr, final String sourceFileName) {
		SourceLocation sLoc = null;

		try {
			final SourcePosition pos = im.getSourcePosition(instrIndex);
			sLoc = SourceLocation.getLocation(sourceFileName, pos.getFirstLine(),
					(pos.getFirstCol() >= 0 ? pos.getFirstCol() : 0), pos.getLastLine(),
					(pos.getLastCol() >= 0 ? pos.getLastCol() : 0));
		} catch (InvalidClassFileException e) {
			Log.warn("No location for instruction " + instr + "\nfor " + Util.methodName(im));
			Log.error(e);
		} catch (NullPointerException e) {
			Log.info("No location for instruction " + instr + "\nfor " + Util.methodName(im));
		} catch (ArrayIndexOutOfBoundsException e) {
			Log.warn("No location for instruction " + instr + "\nfor " + Util.methodName(im));
			Log.error(e);
		}

		return sLoc;
	}

	private SourceLocation getSourceLocation(final IMethod im,
			final StatementWithInstructionIndex ns, final String sourceFileName) {
		final Integer instrIndex = ns.getInstructionIndex();
		final SSAInstruction instr = ns.getInstruction();

		return getSourceLocation(im, instrIndex, instr, sourceFileName);
	}


}
