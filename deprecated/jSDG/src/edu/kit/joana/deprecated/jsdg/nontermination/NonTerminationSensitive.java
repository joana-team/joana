/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.nontermination;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.dataflow.graph.BitVectorOr;
import com.ibm.wala.fixedpoint.impl.DefaultFixedPointSolver;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.Acyclic;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphReachability;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG.Call;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractPDGNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.CallNode;


/**
 * Computes interprocedural nontermination sensitive control dependencies.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
@SuppressWarnings("deprecation")
public class NonTerminationSensitive {

	private final SDG sdg;

	private NonTerminationSensitive(final SDG sdg) {
		this.sdg = sdg;
	}

	/**
	 * Computes all methods that may not terminate due to endless loops or recursion.
	 * Returns all callsites of those methods.
	 * @param sdg
	 * @return Set of callsites calling possible not terminating methods.
	 * @throws CancelException
	 */
	public static Set<CallNode> run(SDG sdg, IProgressMonitor progress) throws CancelException {
		NonTerminationSensitive nts = new NonTerminationSensitive(sdg);
		return nts.compute(progress);
	}

	private Set<CallNode> compute(IProgressMonitor progress) throws CancelException {
		Set<CallNode> calls = HashSetFactory.make();

		Set<CGNode> recursive = findRecursiveMethods(progress);

		Set<CGNode> loop = findLoopingMethods(progress);

		CallGraph cg = sdg.getCallGraph();
		Graph<CGNode> inverted = GraphInverter.invert(cg);
		GraphReachability<CGNode,CGNode> reach = new GraphReachability<CGNode,CGNode>(inverted, x -> true);
		progress.subTask("Searching potential non-returning calls");
		reach.solve(progress);

		Set<CGNode> roots = HashSetFactory.make(loop);
		roots.addAll(recursive);

		OrdinalSet<CGNode> potential = OrdinalSet.empty();

		for (CGNode root : roots) {
			OrdinalSet<CGNode> reached = reach.getReachableSet(root);
			potential = OrdinalSet.unify(potential, reached);
		}

		Set<CGNode> terminating = HashSetFactory.make();
		Set<CGNode> nonTerminating = HashSetFactory.make();

		for (CGNode node : cg) {
			if (potential.contains(node)) {
				nonTerminating.add(node);
			} else {
				terminating.add(node);
			}
		}

		for (Call call : sdg.getAllCalls()) {
			if (nonTerminating.contains(call.callee.getCallGraphNode())) {
				calls.add(call.node);
			}
		}

		progress.done();

		computeControlDependence(calls, progress);

		return calls;
	}

	private void computeControlDependence(Set<CallNode> nonTerm, IProgressMonitor progress) {
		progress.subTask("Compute intraprocedural nontermination sensitive control dependencies");

		for (PDG pdg : sdg.getAllContainedPDGs()) {
			NumberedGraph<AbstractPDGNode> cfg = SDGControlFlowGraph.create(pdg, false);
			for (CallNode call : pdg.getAllCalls()) {
				if (nonTerm.contains(call)) {
					// add a termination sink to all potential non-terminating calls
					AbstractPDGNode art = SDGControlFlowGraph.createArtificialNode();
					cfg.addNode(art);
					cfg.addEdge(call, art);
				}
			}

			NumberedGraph<AbstractPDGNode> cdg = NTSCDGraph.compute(cfg, pdg.getRoot(), pdg.getExit());

			removeArtificialNodes(cdg);

			for (AbstractPDGNode from : cdg) {
				for (Iterator<? extends AbstractPDGNode> it = cdg.getSuccNodes(from); it.hasNext();) {
					AbstractPDGNode to = it.next();
					pdg.addNonterminationSensitiveControlDependency(from, to);
				}
			}

			progress.worked(1);
		}

		progress.done();
	}

	private void removeArtificialNodes(Graph<AbstractPDGNode> cdg) {
		Set<AbstractPDGNode> toRemove = HashSetFactory.make();

		for (AbstractPDGNode node : cdg) {
			if (SDGControlFlowGraph.isArtificial(node)) {
				toRemove.add(node);
			}
		}

		for (AbstractPDGNode node : toRemove) {
			removeNode(cdg, node);
		}
	}

	private static void removeNode(Graph<AbstractPDGNode> cfg, AbstractPDGNode node) {
		Iterator<? extends AbstractPDGNode> itPred = cfg.getPredNodes(node);
		while (itPred.hasNext()) {
			AbstractPDGNode pred = itPred.next();
			Iterator<? extends AbstractPDGNode> itSucc = cfg.getSuccNodes(node);
			while (itSucc.hasNext()) {
				AbstractPDGNode succ = itSucc.next();
				cfg.addEdge(pred, succ);
			}
		}

		cfg.removeNodeAndEdges(node);
	}

	private Set<CGNode> findLoopingMethods(IProgressMonitor progress) throws CancelException {
		CallGraph cg = sdg.getCallGraph();
		Set<CGNode> loops = HashSetFactory.make();

		progress.subTask("Searching methods with potential endless loops");

		for (CGNode node : cg) {
			IR ir = node.getIR();
			if (ir != null) {
				ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = ir.getControlFlowGraph();
				final boolean ac = Acyclic.isAcyclic(cfg, cfg.entry());
				if (!ac && !loopsAreSimple(ir)) {
					loops.add(node);
				}
			} else {
				// Conservatively assume that methods may not terminate, iff we dont
				// have their code
				loops.add(node);
			}

			progress.worked(1);
		}

		progress.done();

		return loops;
	}

	/**
	 * TODO: detect simple loops that are definitely terminating
	 * @param ir
	 * @return
	 * @throws CancelException
	 */
	private boolean loopsAreSimple(IR ir) throws CancelException {
		return false;

//		boolean isSimple = true;
//
//		SSACFG cfg = ir.getControlFlowGraph();
//		ControlFlowGraph<ISSABasicBlock> pcfg = ExceptionPrunedCFG.make(cfg);
//
//		SCCIterator<ISSABasicBlock> it = new SCCIterator<ISSABasicBlock>(cfg);
//		while (it.hasNext()) {
//			Set<ISSABasicBlock> scc = it.next();
//			if (scc.size() > 1) {
//				isSimple &= loopsAreSimple(pcfg, scc);
//			}
//
//			if (!isSimple) {
//				break;
//			}
//		}
//
//		return isSimple;
	}

	@SuppressWarnings("unused")
	private boolean loopsAreSimple(ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg, Set<ISSABasicBlock> scc)
	throws CancelException {
		boolean isSimple = true;

		MutableIntSet sccInts = MutableSparseIntSet.createMutableSparseIntSet(scc.size());
		for (ISSABasicBlock bb : scc) {
			sccInts.add(cfg.getNumber(bb));
		}

		boolean noExit = true;

		for (ISSABasicBlock bb : scc) {
			if (cfg.getSuccNodeCount(bb) > 1 && !cfg.getSuccNodeNumbers(bb).isSubset(sccInts)) {
				noExit = false;
				// if preds are no subset of the scc block we have a jump to the outside of the scc
				SSAInstruction last = bb.getLastInstruction();
				int[] uses = new int[last.getNumberOfUses()];

				for (int i = 0; i < uses.length; i++) {
					uses[i] = last.getUse(i);
				}


				TransitiveDataDependence tdep = new TransitiveDataDependence(scc);
				tdep.solve(null);

				for (ISSABasicBlock bb2 : scc) {
					if (bb2 == bb) {
						continue;
					}

					for (SSAInstruction ii : bb2) {
						for (int use : uses) {
							isSimple &= !tdep.influences(ii, use);
						}

//						IntSet is = tdep.influences((SSAInstruction) ii);
//						System.err.println(ii + ": " + is);

						if (!isSimple) {
							break;
						}
					}

					if (!isSimple) {
						break;
					}
				}

				//TODO check if condition is simple...
//				if (isSimple) {
//					System.err.println("Simple: " + last + " -> " + PrettyWalaNames.methodName(bb.getMethod()));
//				} else {
//					System.err.println("Not Simple: " + last + " -> " + PrettyWalaNames.methodName(bb.getMethod()));
//				}

				if (!isSimple) {
					break;
				}
			}
		}

		return isSimple && !noExit;
	}

	private static class TransitiveDataDependence extends DefaultFixedPointSolver<BitVectorVariable> {

		private final Set<ISSABasicBlock> scc;


		/**
		 * Statement -> Influenced values
		 */
		private final Map<SSAInstruction, BitVectorVariable> transitive;

		/**
		 * SSA Value nr -> Instruction using value
		 */
		private final Map<Integer, Set<SSAInstruction>> reads;

		private TransitiveDataDependence(Set<ISSABasicBlock> scc) {
			this.scc = scc;
			this.transitive = HashMapFactory.make();
			this.reads = HashMapFactory.make();
		}

		@SuppressWarnings("unused")
		private IntSet influences(SSAInstruction instr) {
			final BitVectorVariable bv = transitive.get(instr);
			final IntSet set = bv.getValue();
			if (set == null) {
				return IntSetUtil.make();
			} else {
				return IntSetUtil.getDefaultIntSetFactory().makeCopy(set);
			}
		}

		private boolean influences(SSAInstruction instr, int var) {
			BitVectorVariable bv = transitive.get(instr);

			return bv.get(var);
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractFixedPointSolver#initializeVariables()
		 */
		@Override
		protected void initializeVariables() {
			for (ISSABasicBlock bb : scc) {
				for (SSAInstruction ssa : bb) {
					BitVectorVariable bv = new BitVectorVariable();
					for (int i = 0; i < ssa.getNumberOfDefs(); i++) {
						final int def = ssa.getDef(i);
						bv.set(def);
					}

					transitive.put(ssa, bv);

					for (int i = 0; i < ssa.getNumberOfUses(); i++) {
						Integer use = ssa.getUse(i);
						Set<SSAInstruction> set = reads.get(use);
						if (set == null) {
							set = HashSetFactory.make();
							reads.put(use, set);
						}
						set.add(ssa);
					}
				}
			}

			final BitVectorOr or = new BitVectorOr(new BitVector());

			for (Entry<SSAInstruction, BitVectorVariable> entry : transitive.entrySet()) {
				SSAInstruction instr = entry.getKey();
				BitVectorVariable var = entry.getValue();

				for (int i = 0; i < instr.getNumberOfDefs(); i++) {
					final int def = instr.getDef(i);
					Set<SSAInstruction> users = reads.get(def);
					if (users != null) {
						for (SSAInstruction user : users) {
							// instr influences also all variables that are written by statements that use the output of instr
							BitVectorVariable varUser = transitive.get(user);
							newStatement(var, or, varUser, true, false);
						}
					}
				}
			}
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.fixedpoint.impl.AbstractFixedPointSolver#initializeWorkList()
		 */
		@Override
		protected void initializeWorkList() {
			addAllStatementsToWorkList();
		}

		@Override
		protected BitVectorVariable[] makeStmtRHS(int size) {
			return new BitVectorVariable[size];
		}

	}

	private Set<CGNode> findRecursiveMethods(IProgressMonitor progress) throws CancelException {
		CallGraph cg = sdg.getCallGraph();
		GraphReachability<CGNode,CGNode> reach = new GraphReachability<CGNode,CGNode>(cg, x -> true);
		progress.subTask("Searching recursive methods");
		reach.solve(progress);

		Set<CGNode> recursive = HashSetFactory.make();

		for (CGNode node : cg) {
			OrdinalSet<CGNode> rset = reach.getReachableSet(node);
			for (CGNode target : rset) {
				if (target != node) {
					OrdinalSet<CGNode> tset = reach.getReachableSet(target);
					if (tset.contains(node)) {
						recursive.add(node);
						break;
					}
				}
			}

			progress.worked(1);
		}

		progress.done();

		return recursive;
	}
}
