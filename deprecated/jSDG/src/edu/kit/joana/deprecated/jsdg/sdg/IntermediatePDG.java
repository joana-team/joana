/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cfg.ExceptionPrunedCFG;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.exceptions.ExceptionPrunedCFGAnalysis;
import edu.kit.joana.deprecated.jsdg.sdg.controlflow.ControlDependenceGraph;
import edu.kit.joana.deprecated.jsdg.sdg.controlflow.ControlDependenceGraph.Edge;
import edu.kit.joana.deprecated.jsdg.sdg.controlflow.IgnoreNonTerminationCFG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CatchNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.EntryNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.NormalNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ObjectField;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamSet;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParameter;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.IPointerAnalysis;
import edu.kit.joana.deprecated.jsdg.util.Debug;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Log.LogLevel;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.deprecated.jsdg.wala.BytecodeLocation;
import edu.kit.joana.deprecated.jsdg.wala.SourceLocation;

/**
 * Contains most of the intraprocedural pdg creation except the tricky part
 * of interprocedural interface calculation (adjusting form-in/out nodes using
 * object trees). It provides the data dependency and control dependency
 * calculation.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public abstract class IntermediatePDG extends AbstractPDG {

	private final CGNode method;
	private final IMethod imethod;
	protected final IPointerAnalysis pta;
	private final IR ir;
	private final HeapModel model;
	private final CallGraph cg;
	private final IClassHierarchy hierarchy;
	private final String sourceFile;

	private final Map<AbstractPDGNode, SourceLocation> locations;

	private final EntryNode root;
	private AbstractParameterNode exit;

	/*
	 * Not final because it is only created iff theres an exception thrown -
	 * which is not known at the time the constructor is called.
	 */
	private AbstractParameterNode exitThrow;

	private final boolean ignoreExceptions;
	private final ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock> epa;

	public final boolean isIgnoreExceptions() {
		return ignoreExceptions;
	}

	public final String getSourceFileName() {
		return sourceFile;
	}

	public BytecodeLocation getBytecodeLocation(AbstractPDGNode node) {
		assert (node.getPdgId() == getId()) : "Node not part of this pdg";

		BytecodeLocation bcLoc = null;

		if (node.isParameterNode()) {
			final AbstractParameterNode param = (AbstractParameterNode) node;
			final String bcName = param.getBytecodeName();
			final int paramType;
			if (param.isStaticField()) {
				paramType = BytecodeLocation.STATIC_FIELD;
			} else if (param.isRoot()) {
				paramType = BytecodeLocation.ROOT_PARAMETER;
			} else if (param.isArray()) {
				paramType = BytecodeLocation.ARRAY_FIELD;
			} else if (param.isObjectField()) {
				paramType = BytecodeLocation.OBJECT_FIELD;
			} else {
				paramType = BytecodeLocation.UNDEFINED_POS_IN_BYTECODE;
			}

			bcLoc = new BytecodeLocation(bcName, paramType);
		} else {
			final String bcName = imethod.getSignature();
			final SSAInstruction instr = getInstructionForNode(node);
			if (instr != null) {
				if (instr instanceof SSAPhiInstruction) {
					// node bytecode equivalent for phi instructions.
					bcLoc = new BytecodeLocation(bcName, BytecodeLocation.UNDEFINED_POS_IN_BYTECODE);
				} else {
					final int bcIndex = getInstrIndex(instr);
					bcLoc = new BytecodeLocation(bcName, bcIndex);
				}
			} else {
				// no instruction found
				bcLoc = new BytecodeLocation(bcName, BytecodeLocation.UNDEFINED_POS_IN_BYTECODE);
			}
		}

		return bcLoc;
	}

	private final int getInstrIndex(SSAInstruction instr) {
		if (ir != null && (imethod instanceof ShrikeBTMethod)) {
			ShrikeBTMethod sbtm = (ShrikeBTMethod) imethod;

			SSAInstruction[] arr = ir.getInstructions();
			for (int index = 0; index < arr.length; index++) {
				if (arr[index] == instr) {
					int bcIndex = -1;

					try {
						bcIndex = sbtm.getBytecodeIndex(index);
			        } catch (NullPointerException e) {
			        	// some methods (reflection loader - getClass) do not have a byte location
					} catch (InvalidClassFileException e) {
					} catch (ArrayIndexOutOfBoundsException e) {
						Log.error("WALA: Index out of bounds. No matching bytecode index for ssa instruction index "
								+ index + " of instruction " + instr);
					}

					return bcIndex;
				}
			}
		}

		return -1;
	}

	public SourceLocation getLocation(AbstractPDGNode node) {
		assert (node.getPdgId() == getId()) : "Node not part of this pdg";

		SourceLocation sloc = locations.get(node);

		if (sloc == null && node instanceof IParameter) {
			/**
			 * Special location search for parameter nodes
			 * Formal-in parameter nodes are mapped to the location of the
			 * parameter declaration of the method they belong to. This location
			 * is saved in the local parameter nodes.
			 *
			 * Actual parameter nodes are mapped to the location of the call
			 * instruction they belong to.
			 */
			IParameter p = (IParameter) node;
			if (p.isIn() && p.isFormal() && p.isOnHeap()) {
				IParamSet<? extends AbstractParameterNode> refs = getParamModel().getRefParams();
				for (AbstractParameterNode param : refs) {
					if (!param.isOnHeap()) {
						sloc = locations.get(param);
						break;
					}
				}
			} else if (p.isActual()) {
				SSAInstruction instr = getInstructionForNode(node);
				List<AbstractPDGNode> nodes = getNodesForInstruction(instr);
				if (nodes != null) {
					for (AbstractPDGNode n : nodes) {
						if (!n.isParameterNode()) {
							sloc = locations.get(n);
							break;
						}
					}
				}
			}
		}


		return sloc;
	}

	public void addLocation(AbstractPDGNode node, SourceLocation loc) {
		assert (node.getPdgId() == getId()) : "Node not part of this pdg";
		assert (loc != null);

		locations.put(node, loc);
	}


	IntermediatePDG(CGNode method, int id, IPointerAnalysis pta, CallGraph cg, boolean ignoreExceptions,
			ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock> epa) {
		super(id);
		this.method = method;
		if (method == null) {
			throw new IllegalArgumentException("At least a single call graph node has to be supplied.");
		}

		this.imethod = method.getMethod();

		this.pta = pta;
		this.cg = cg;
		this.ir = method.getIR();
		this.model = pta.getHeapModel();
		this.hierarchy = model.getClassHierarchy();
		this.locations = HashMapFactory.make();
		this.root = makeEntry(Util.methodName(imethod));

		this.exit = null;

		this.exitThrow = null;

		IClass c = imethod.getDeclaringClass();
		this.sourceFile = Util.sourceFileName(c.getName());

		this.ignoreExceptions = ignoreExceptions;
		this.epa = epa;
	}

	void createMissingDataDependencies(ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg) {
		createUnresolvedDataDependencies();
		fixPhiNodeConstantPropagation(cfg);
	}

	/**
	 * <pre>
	 * b = false;
	 * if (x > 42) {
	 *     b = true;
	 * }
	 *
	 * print b;
	 * </pre>
	 *
	 * Results in the following control flow graph:
	 *
	 * <pre>
	 * entry -> if (x > 42) -\
	 *       \-----------------> v1 = phi const false, const true -> print v1
	 * </pre>
	 *
	 * The value of the phi variable v1 has no dependency to the code in the if
	 * clause. The only dependency can be derived through the path that was taken
	 * to reach the phi. If control flow comes from the if clause, then v1 is true
	 * else v1 is false. This dependency is neither included by data nor control dependencies.
	 * We have to add them manually.
	 *
	 * For each phi node, we look at the predecessors and determine the expression/node
	 * that is responsible for this cfg path. An artificial data dependency is then added to
	 * this node.
	 *
	 * @param cfg The control flow graph.
	 */
	private void fixPhiNodeConstantPropagation(ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg) {
		for (IExplodedBasicBlock bb : cfg) {
			Iterator<SSAPhiInstruction> it = bb.iteratePhis();
			if (it != null && it.hasNext()) {
				while (it.hasNext()) {
					SSAPhiInstruction phi = it.next();
					AbstractPDGNode phiNode = getMainNodeForInstruction(phi);

					// find predecessors with instruction - node
					Set<IExplodedBasicBlock> preds = getPredsWithPDGNode(cfg, bb);
					for (IExplodedBasicBlock pred : preds) {
						if (pred.isEntryBlock()) {
							AbstractPDGNode predNode = getRoot();
							addControlDependency(predNode, phiNode, true);
						} else {
							SSAInstruction instr = pred.getInstruction();
							assert instr != null : "No instruction for " + pred;
							AbstractPDGNode predNode = getMainNodeForInstruction(instr);
							assert predNode != null : "No node for instruction " + instr;

							if (cfg.getSuccNodeCount(pred) > 1) {
								// add data dep directly to predecessor
								addDataDependency(predNode, phiNode);
							} else {
								// add data dep to control flow dominator
								Set<AbstractPDGNode> cdPreds = getPredsWithControlDep(predNode);
								for (AbstractPDGNode cd : cdPreds) {
									addDataDependency(cd, phiNode);
								}
							}
						}
					}
				}
			}
		}
	}

	private Set<IExplodedBasicBlock> getPredsWithPDGNode(ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg, IExplodedBasicBlock bb) {
		Set<IExplodedBasicBlock> preds = new HashSet<IExplodedBasicBlock>();

		Iterator<IExplodedBasicBlock> it = cfg.getPredNodes(bb);
		while (it.hasNext()) {
			IExplodedBasicBlock pred = it.next();
			if (pred.isEntryBlock()) {
				preds.add(pred);
			} else if (pred.getInstruction() != null) {
				SSAInstruction instr = pred.getInstruction();
				AbstractPDGNode node = getMainNodeForInstruction(instr);
				if (node != null) {
					preds.add(pred);
				} else {
					preds.addAll(getPredsWithPDGNode(cfg, pred));
				}
			}
		}

		return preds;
	}

	private Set<AbstractPDGNode> getPredsWithControlDep(AbstractPDGNode node) {
		Set<AbstractPDGNode> cdPreds = new HashSet<AbstractPDGNode>();

		Iterator<AbstractPDGNode> nodePredIt = getPredNodes(node, EdgeType.CD_EX);
		while (nodePredIt.hasNext()) {
			AbstractPDGNode cdPred = nodePredIt.next();
			cdPreds.add(cdPred);
		}
		nodePredIt = getPredNodes(node, EdgeType.CD_TRUE);
		while (nodePredIt.hasNext()) {
			AbstractPDGNode cdPred = nodePredIt.next();
			cdPreds.add(cdPred);
		}
		nodePredIt = getPredNodes(node, EdgeType.CD_FALSE);
		while (nodePredIt.hasNext()) {
			AbstractPDGNode cdPred = nodePredIt.next();
			cdPreds.add(cdPred);
		}
		nodePredIt = getPredNodes(node, EdgeType.UN);
		while (nodePredIt.hasNext()) {
			AbstractPDGNode cdPred = nodePredIt.next();
			cdPreds.add(cdPred);
		}

		return cdPreds;
	}

	/**
	 * Adds unresolved data dependencies to the pdg. These are either forward
	 * references to ssa vars or references to phi values.
	 */
	private void createUnresolvedDataDependencies() {
		// add unresolved (loop carried/forward) data dependencies
		for (int fromKey : getUnresolvedSSAVars()) {
			List<AbstractPDGNode> nodes = getUnresolvedDDs(fromKey);
			Set<AbstractPDGNode> fromNodes = getDefinesVar(fromKey);

			if (Debug.Var.DUMP_SSA.isSet()) {
				if (fromNodes == null) {
					Util.dumpSSA(ir, Log.getStream(LogLevel.DEBUG));
					Log.debug("PHI Values:");
					Util.dumpPhiSSA(ir, Log.getStream(LogLevel.DEBUG));
					Util.dumpCatchExceptionSSA(ir,
							Log.getStream(LogLevel.DEBUG));
				}

			}

			if (fromNodes != null) {
				for (AbstractPDGNode to : nodes) {
					for (AbstractPDGNode from : fromNodes) {
						if (from != to) {
							addDataDependency(from, to, fromKey);
						}
					}
				}
			} else {
				Log.warn("No node defining value no. " + fromKey +
					" found - skipping data dependencies.");
			}
		}
	}

	ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> buildCFG(IProgressMonitor monitor) throws CancelException, PDGFormatException {
		ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg;

		if (!ignoreExceptions && epa != null) {
			try {
				cfg = epa.getPruned(method, monitor);
				cfg = new IgnoreNonTerminationCFG<SSAInstruction, IExplodedBasicBlock>(cfg, ignoreExceptions);
			} catch (UnsoundGraphException e) {
				throw new PDGFormatException(e);
			}
		} else if (ignoreExceptions) {
			cfg = ExplodedControlFlowGraph.make(ir);
			cfg = new IgnoreNonTerminationCFG<SSAInstruction, IExplodedBasicBlock>(cfg, ignoreExceptions);
			cfg = ExceptionPrunedCFG.make(cfg);
		} else {
			cfg = ExplodedControlFlowGraph.make(ir);
			cfg = new IgnoreNonTerminationCFG<SSAInstruction, IExplodedBasicBlock>(cfg, ignoreExceptions);
		}

		if (cfg == null || cfg.getNumberOfNodes() == 0) {
			Log.warn("No control flow graph for " + Util.methodName(imethod) +
					" found. Aborting creation of control dependencies.");

			return null;
		}

		return cfg;
	}

	void createControlDependencies(ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg,
			IProgressMonitor monitor) throws PDGFormatException, CancelException {
		ControlDependenceGraph<IExplodedBasicBlock> cdg = ControlDependenceGraph.build(cfg, cfg.entry(), cfg.exit());

		addControlDepsToGraph(cdg, monitor);
	}

	private void addControlDepsToGraph(ControlDependenceGraph<IExplodedBasicBlock> cdg, IProgressMonitor monitor)
	throws PDGFormatException {
		for (IExplodedBasicBlock bb : cdg) {
			// Nothing is control dependent on the exit block -> we skip it
			if (bb.isExitBlock()) {
				continue;
			} else if (bb.isCatchBlock()) {
				final SSAGetCaughtExceptionInstruction catchInstr = bb.getCatchInstruction();
				if (catchInstr != null) {
					CatchNode srcCatch = getCatchForBB(catchInstr.getBasicBlockNumber());
					if (srcCatch != null) {
						if (!srcCatch.isCatchesSet()) {
							srcCatch.setCatches(bb.getCaughtExceptionTypes());
						}

						for (Iterator<IExplodedBasicBlock> succ = cdg.getSuccNodes(bb); succ.hasNext();) {
							IExplodedBasicBlock bb2 = succ.next();

							addControlDependencyForBasicBlock(bb2, srcCatch, false);
						}
					} else {
						// no node for catchblock!!
						Log.warn("No node for catch block....");
					}
				}
			} else {
				/**
				 * add dependencies from the wala control dependence graph (cdg).
				 * this graph contains dependencies between basic blocks
				 */
				Iterator<IExplodedBasicBlock> succ = cdg.getSuccNodes(bb);
				if (succ.hasNext()) {
					AbstractPDGNode src = getLastNodeForBasicBlock(bb);
					if (src == null) {
						Log.warn("No node for BB " + bb);
						continue;
					}

					while (succ.hasNext()) {
						IExplodedBasicBlock bb2 = succ.next();
						final boolean cdTrue = !cdg.hasEdge(bb, bb2, Edge.CD_FALSE);
						addControlDependencyForBasicBlock(bb2, src, cdTrue);
					}
				}
			}

		}


		if (!exit.isVoid()) {
			connectReturnsWithExit();
		}

		if (!ignoreExceptions) {
			connectThrowsWithExitAndCatch();
		} else {
			connectDummyCatchBlocks();
		}
	}

	/**
	 * Simply add all elements of a catch block as control dependent on the root
	 * node. This is only done when exceptions are ignored.
	 */
	private void connectDummyCatchBlocks() {
		assert (ignoreExceptions);

		SSACFG cfg = ir.getControlFlowGraph();
		BitVector catches = cfg.getCatchBlocks();
		if (!catches.isZero()) {
			SSAInstruction[] instrs= ir.getInstructions();
			BitVectorIntSet cSet = new BitVectorIntSet(catches);
			IntIterator it = cSet.intIterator();
			while (it.hasNext()) {
				int bbNum = it.next();
				ISSABasicBlock bb = cfg.getBasicBlock(bbNum);
				for (int i = bb.getFirstInstructionIndex(); i <= bb.getLastInstructionIndex(); i++) {
					SSAInstruction ii = instrs[i];
					if (ii != null) {
						AbstractPDGNode node = getMainNodeForInstruction(ii);
						if (node != null) {
							addUnconditionalControlDependency(root, node);
						}
					}
				}
			}
		}

		for (Iterator<SSAInstruction> it = ir.iterateCatchInstructions(); it.hasNext();) {
			SSAInstruction ii = it.next();
			if (ii != null) {
				AbstractPDGNode node = getMainNodeForInstruction(ii);
				if (node != null) {
					addUnconditionalControlDependency(root, node);
				}
			}
		}
	}

	/**
	 * Some instructions correspond to a list of nodes. E.g. calls have a call
	 * node and multiple act-in/out nodes. When building control dependencies
	 * between instructions, we only need the main node (e.g. the call one).
	 * This method helps extracting it.
	 * @param i instruction
	 * @return main node of the instruction
	 */
	public AbstractPDGNode getMainNodeForInstruction(SSAInstruction i) {
		AbstractPDGNode node = null;

		List<AbstractPDGNode> nodeList = getNodesForInstruction(i);
		if (nodeList != null && nodeList.size() > 0) {
        	// only use the first node in the the, the others are
        	// form-in/out (or equiv) nodes

			if (!(i instanceof SSAAbstractInvokeInstruction) && nodeList.size() > 1) {
				throw new IllegalStateException(i + " has " + nodeList.size() + " nodes.");
			}

			node = nodeList.get(0);
		}

		return node;
	}


	private AbstractPDGNode getLastNodeForBasicBlock(IBasicBlock<?> bb) {
		AbstractPDGNode node = null;

		if (bb.isEntryBlock()) {
			return root;
		} else if (bb.isCatchBlock()) {
			return getCatchForBB(bb.getNumber());
		}

		for (int i = bb.getLastInstructionIndex();
				node == null && i >= bb.getFirstInstructionIndex(); i--) {
			SSAInstruction instr = ir.getInstructions()[i];
			if (instr != null) {
				node = getMainNodeForInstruction(instr);
			}
		}

		if (Debug.Var.OLD_ASSERT.isSet()) {
			if (node == null) {
				Log.warn("No node found for basic block " + bb);
				Util.dumpSSA(ir, bb, Log.getStream(LogLevel.DEBUG));
			}
		}

		return node;
	}

	private void addControlDependencyForBasicBlock(ISSABasicBlock bb, AbstractPDGNode src, boolean type) {
		if (bb.isExitBlock()) {
			addControlDependency(src, exit, type);
		} else if (bb.isEntryBlock()) {
			addControlDependency(src, root, type);
		}

		for (SSAInstruction ii : bb) {
			if (ii instanceof SSAPhiInstruction || ii instanceof SSAGotoInstruction) {
				// skip phis and gotos - they do not have any  pdg nodes assigned
				continue;
			}

            AbstractPDGNode destNode = getMainNodeForInstruction(ii);
            // on call instructions this may return multiple call nodes. So we
            // introduced compound nodes for calls with multiple targets.

            if (destNode != null) {
            	if (src != destNode) {
            		addControlDependency(src, destNode, type);
            	} else {
            		Log.warn("Trying to add selfrecursive control dep to " + src);
            	}
            } else {
            	Log.error("No control dependency created: No node found for " + ii + " in " + toString());
            }
		}
	}

	private void connectReturnsWithExit() {
		for (NormalNode ret : getReturns()) {
			addControlDependency(ret, exit, true);
			if (getPredNodeCount(ret, EdgeType.CD_EX) == 0 &&
				getPredNodeCount(ret, EdgeType.CD_TRUE) == 0 &&
				getPredNodeCount(ret, EdgeType.CD_FALSE) == 0 &&
				getPredNodeCount(ret, EdgeType.UN) == 0 &&
				getPredNodeCount(ret, EdgeType.CE) == 0) {
				// return instruction is not control dependent on anything,
				// so we add at least the entry node
				Log.warn("Return statement without ingoing control dep: " + ret);
				addUnconditionalControlDependency(root, ret);
			}
		}
	}

	private void connectThrowsWithExitAndCatch() {
		assert (!ignoreExceptions);

		final SSACFG cfg = ir.getControlFlowGraph();
		BitVector catches = cfg.getCatchBlocks();
		BitVectorIntSet set = new BitVectorIntSet(catches);
		set.foreach(new IntSetAction() {

			public void act(int x) {
				ISSABasicBlock catchbb = cfg.getNode(x);
				CatchNode catchn = getCatchForBB(catchbb.getNumber());
				if (catchn == null) {
					// this may happen as our control flow analysis detects impossible flow
					// and does not create nodes for unreachable catch blocks...
					return;
				}

				for (Iterator<ISSABasicBlock> it = cfg.getPredNodes(catchbb); it.hasNext(); ) {
					ISSABasicBlock pred = it.next();
					AbstractPDGNode node = getLastNodeForBasicBlock(pred);
					if (node != null && node != catchn) {
						addExceptionControlDependency(node, catchn);
					}
				}
			}
		});

		for (NormalNode ret : getThrows()) {
			addExceptionControlDependency(ret, exitThrow);
		}
	}

	void createNodesForStub() {
		exit = getParamModel().makeExit();
		if (exit != null) {
			addParameterChildDependency(root, exit);
		}

		if (!ignoreExceptions) {
			exitThrow = getParamModel().makeExceptionalExit();
			addParameterChildDependency(root, exitThrow);
		}
	}

	/**
	 * Creates pdg nodes using the intermediate representation and the control flow graph
	 */
	void createNodesFromIR(ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg) {
		if (cfg == null) {
			throw new IllegalArgumentException("A method needs an cfg.");
		}

		exit = getParamModel().makeExit();
		if (exit != null) {
			addParameterChildDependency(root, exit);
		}

		if (!ignoreExceptions) {
			exitThrow = getParamModel().makeExceptionalExit();
			addParameterChildDependency(root, exitThrow);
		}

		SSAInstruction[] irs = ir.getInstructions();
		for (int i = 0; i < irs.length; i++) {
			SSAInstruction ssa = irs[i];
			if (ssa != null) {
				addSSAIndex(ssa, i);
			}
		}

		PDGNodeCreationVisitor ncv = new PDGNodeCreationVisitor(this, locations, ignoreExceptions);
		for (IExplodedBasicBlock block : cfg) {
			SSAInstruction ssa = block.getInstruction();
			if (ssa != null) {
				ssa.visit(ncv);

				if (ssa instanceof SSAPhiInstruction) {
					throw new IllegalStateException();
				} else if (ssa instanceof SSAGetCaughtExceptionInstruction) {
					throw new IllegalStateException();
				}
			}

			Iterator<SSAPhiInstruction> itPhi = block.iteratePhis();
			SSAPhiInstruction phi = null;
			while (itPhi != null && itPhi.hasNext()) {
				phi = itPhi.next();
				phi.visit(ncv);
			}

			assert ssa == null || phi == null : "ssa: " + ssa + " - phi: " + phi;

			if (!ignoreExceptions && block.isCatchBlock()) {
				SSAGetCaughtExceptionInstruction catchInstr = block.getCatchInstruction();
				if (catchInstr != null) {
					catchInstr.visit(ncv);
				}
			}
		}

		ncv.done();
	}

	public PointerKey getPointerKey(int ssa) {
		Set<PointerKey> pks = HashSetFactory.make();

		PointerKey pk = model.getPointerKeyForLocal(method, ssa);
		pks.add(pk);

		return pk;
	}

	public PointerKey getPointerKey(ParameterField field) {
		assert (field.isField());
		assert (field.isStatic());

		IField ifield = ((ObjectField) field).getField();

		return model.getPointerKeyForStaticField(ifield);
	}

	/**
	 * points to set for static fields
	 * @param field
	 * @return
	 */
	public OrdinalSet<InstanceKey> getPointsToSet(ParameterField field) {
		assert (field.isField());
		assert (field.isStatic());
			//Assertions._assert(!field.isPrimitiveType());

		IField ifield = ((ObjectField) field).getField();
		PointerKey pkField = pta.getHeapModel().getPointerKeyForStaticField(ifield);

		return pta.getPointsToSet(pkField);
	}

	public OrdinalSet<InstanceKey> getPointsToSet(int ssa) {
		PointerKey pk = getPointerKey(ssa);

		assert (pk != null) : "No PointerKey for ssa var no. " +	ssa + " in method \'"
			+ Util.methodName(imethod) + "\'";

		OrdinalSet<InstanceKey> pts = pta.getPointsToSet(pk);

		assert (pts != null) : "No Points-to set for PointerKey " + pk + " of ssa var no. " + ssa + " found.";

		return pts;
	}

	public IMethod getMethod() {
		return imethod;
	}

	public CGNode getCallGraphNode() {
		return method;
	}

	public CallGraph getCallGraph() {
		return cg;
	}

	public EntryNode getRoot() {
		return root;
	}

	public AbstractParameterNode getExit() {
		if (exit == null) {
			exit = getParamModel().makeExit();
		}

		return exit;
	}

	public AbstractParameterNode getExceptionalExit() {
		if (isIgnoreExceptions()) {
			throw new UnsupportedOperationException("No exception nodes here, when exceptions are ignored!");
		}

		return exitThrow;
	}

	public IR getIR() {
		return ir;
	}

	public IClassHierarchy getHierarchy() {
		return hierarchy;
	}

	public String toString() {
		return "PDG of " + Util.methodName(imethod);
	}

}
