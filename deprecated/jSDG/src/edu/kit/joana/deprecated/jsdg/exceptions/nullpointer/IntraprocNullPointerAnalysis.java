/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.exceptions.nullpointer;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.dataflow.graph.DataflowSolver;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cfg.PrunedCFG;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstruction.IVisitor;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SparseNumberedGraph;

import edu.kit.joana.deprecated.jsdg.exceptions.ExceptionPrunedCFGAnalysis;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;

/**
 * Intraprocedural dataflow analysis to detect impossible NullPointerExceptions.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
class IntraprocNullPointerAnalysis<T extends ISSABasicBlock> {

	// by convention of WALA: the 0-th parameter of a non-static method is "this".
	private static final int THIS_PTR_VAL_NUM = 0;

	private final Set<TypeReference> ignoreExceptions;
	private final CGNode method;
	private final ControlFlowGraph<SSAInstruction, T> cfg;
	private final int maxVarNum;
	private final IR ir;
	private ControlFlowGraph<SSAInstruction, T> pruned = null;

	IntraprocNullPointerAnalysis(CGNode method, ControlFlowGraph<SSAInstruction, T> cfg, TypeReference[] ignoreExceptions) {
		this.method = method;
		this.cfg = cfg;
		this.ir = method.getIR();
		if (ir == null || ir.isEmptyIR()) {
			maxVarNum = -1;
		} else {
			maxVarNum = ir.getSymbolTable().getMaxValueNumber();
		}
		this.ignoreExceptions = new HashSet<TypeReference>(ignoreExceptions.length);
		for (TypeReference tRef : ignoreExceptions) {
			this.ignoreExceptions.add(tRef);
		}
	}

	void run(IProgressMonitor progress) throws CancelException {
		if (pruned == null) {
			final IR ir = method.getIR();
			if (ir == null || ir.isEmptyIR()) {
				pruned = cfg;
			} else {
				NullPointerFrameWork<T> problem = new NullPointerFrameWork<T>(cfg, ir);
				int[] paramValNum = ir.getParameterValueNumbers();
				NullPointerSolver<T> solver;

				if (method.getMethod().isStatic()) {
					solver = new NullPointerSolver<T>(problem, maxVarNum, paramValNum);
				} else {
					solver = new NullPointerSolver<T>(problem, maxVarNum, paramValNum, THIS_PTR_VAL_NUM);
				}

				if (solver.solve(progress)) {
					// we were able to remove some exceptions
					Graph<T> deleted = createDeletedGraph(solver);
					NegativeGraphFilter<T> filter = new NegativeGraphFilter<T>(deleted);

					printDetails(deleted);

					pruned = PrunedCFG.make(cfg, filter);

					//DEBUG start

//					if (method.getMethod().getName().toString().contains("simpleTests")) {
//						DebugUtil.CFGNodeDec nodeDec = new DebugUtil.CFGNodeDec(solver);
//						DebugUtil.dumpCFGorigExpl(cfg, "np-", nodeDec);
//						DebugUtil.dumpCFGorigExpl(pruned, "optimized-", nodeDec);
//					}

					//DEBUG end


				} else {
					pruned = cfg;
				}
			}
		}
	}

	// Debug statistics
	private static final Set<CGNode> visited = new HashSet<CGNode>();

	private void printDetails(Graph<T> deleted) {
		if (!visited.contains(method)) {
			visited.add(method);

			int deletedEdges = 0;
			for (T node : deleted) {
				deletedEdges += deleted.getSuccNodeCount(node);
			}

			int originalEdges = 0;
			for (T node : cfg) {
				originalEdges += cfg.getSuccNodeCount(node);
			}

			int totalPEI = 0;
			Iterator<SSAInstruction> it = ir.iterateAllInstructions();
			while (it.hasNext()) {
				SSAInstruction instr = it.next();
				if (instr.isPEI()) {
					totalPEI++;
					Collection<TypeReference> exceptions = instr.getExceptionTypes();
					if (exceptions != null) {
						for (TypeReference tref : exceptions) {
							Integer num = ExceptionPrunedCFGAnalysis.COUNT_EXCEPTIONS.get(tref);
							if (num == null) {
								num = 0;
							}
							num++;
							ExceptionPrunedCFGAnalysis.COUNT_EXCEPTIONS.put(tref, num);
						}
					}
				}
			}

			DecimalFormat df = new DecimalFormat("00.00");
			double percent = ((double) deletedEdges / (double) originalEdges) * 100.0;
			double percentExc = ((double) deletedEdges / (double) totalPEI) * 100.0;
			Log.info("EXC: " + df.format(percent) + "% - edges: " + originalEdges + " deleted: " + deletedEdges
				+ " total exc: " + totalPEI + "(" + df.format(percentExc) + "%) - " + Util.methodName(method.getMethod()));

			ExceptionPrunedCFGAnalysis.PERCENT.put(method, percent);
			ExceptionPrunedCFGAnalysis.PERCENT_PIE.put(method, percentExc);
		}
	}

	private Graph<T> createDeletedGraph(NullPointerSolver<T> solver) {
		NegativeCFGBuilderVisitor nCFGbuilder = new NegativeCFGBuilderVisitor(solver);
		for (T bb : cfg) {
			nCFGbuilder.work(bb);
		}

		Graph<T> deleted = nCFGbuilder.getNegativeCFG();
		return deleted;
	}

	ControlFlowGraph<SSAInstruction, T> getPrunedCfg() {
		if (pruned == null) {
			throw new IllegalStateException("Run analysis first! (call run())");
		}

		return pruned;
	}

	private static class NullPointerSolver<T extends ISSABasicBlock> extends DataflowSolver<T, EdgeState> {

		private final int maxVarNum;
		private final int[] paramVarNum;
		private final int thisPtrVarNum;

		private static final int NO_THIS_PTR = -1;

		private NullPointerSolver(NullPointerFrameWork<T> problem, int maxVarNum, int[] paramVarNum) {
			this(problem, maxVarNum, paramVarNum, NO_THIS_PTR);
		}

		private NullPointerSolver(NullPointerFrameWork<T> problem, int maxVarNum, int[] paramVarNum, int thisPtrVarNum) {
			super(problem);
			this.maxVarNum = maxVarNum;
			this.paramVarNum = paramVarNum;
			this.thisPtrVarNum = thisPtrVarNum;
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.dataflow.graph.DataflowSolver#makeEdgeVariable(java.lang.Object, java.lang.Object)
		 */
		@Override
		protected EdgeState makeEdgeVariable(T src, T dst) {
			return (thisPtrVarNum != NO_THIS_PTR
					? new EdgeState(maxVarNum, paramVarNum, thisPtrVarNum)
					: new EdgeState(maxVarNum, paramVarNum));
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.dataflow.graph.DataflowSolver#makeNodeVariable(java.lang.Object, boolean)
		 */
		@Override
		protected EdgeState makeNodeVariable(T n, boolean IN) {
			return (thisPtrVarNum != NO_THIS_PTR
					? new EdgeState(maxVarNum, paramVarNum, thisPtrVarNum)
					: new EdgeState(maxVarNum, paramVarNum));
		}

		@Override
		protected EdgeState[] makeStmtRHS(int size) {
			return new EdgeState[size];
		}

	}

	private class NegativeCFGBuilderVisitor implements IVisitor {

		private final Graph<T> deleted;
		private final NullPointerSolver<T> solver;

		private NegativeCFGBuilderVisitor(NullPointerSolver<T> solver) {
			this.solver = solver;
			this.deleted = new SparseNumberedGraph<T>(2);
			for (T bb : cfg) {
				deleted.addNode(bb);
			}
		}

		private EdgeState currentState;
		private T  currentBlock;

		public void work(T bb) {
			if (bb == null) {
				throw new IllegalArgumentException("Null not allowed");
			} else if (!cfg.containsNode(bb)) {
				throw new IllegalArgumentException("Block not part of current CFG");
			}

			SSAInstruction instr = NullPointerTransferFunctionProvider.getRelevantInstruction(bb);

			if (instr != null) {
				currentState = solver.getIn(bb);
				currentBlock = bb;
				instr.visit(this);
				currentState = null;
				currentBlock = null;
			}
		}

		public Graph<T> getNegativeCFG() {
			return deleted;
		}

		private boolean isOnlyNullPointerExc(SSAInstruction instr) {
			assert instr.isPEI();

			Collection<TypeReference> exc = instr.getExceptionTypes();
			Set<TypeReference> myExcs = new HashSet<TypeReference>(exc);
			myExcs.removeAll(ignoreExceptions);

			return myExcs.size() == 1 && myExcs.contains(TypeReference.JavaLangNullPointerException);
		}

		private void removeImpossibleSuccessors(SSAInstruction instr, int varNum) {
			if (isOnlyNullPointerExc(instr)) {
				if (currentState.isNeverNull(varNum)) {
					for (T succ : cfg.getExceptionalSuccessors(currentBlock)) {
						deleted.addEdge(currentBlock, succ);
					}
				} else if (currentState.isAlwaysNull(varNum)) {
					for (T succ : cfg.getNormalSuccessors(currentBlock)) {
						deleted.addEdge(currentBlock, succ);
					}
				}
			}
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitArrayLength(com.ibm.wala.ssa.SSAArrayLengthInstruction)
		 */
		@Override
		public void visitArrayLength(SSAArrayLengthInstruction instruction) {
			int varNum = instruction.getArrayRef();
			removeImpossibleSuccessors(instruction, varNum);
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitArrayLoad(com.ibm.wala.ssa.SSAArrayLoadInstruction)
		 */
		@Override
		public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
			int varNum = instruction.getArrayRef();
			removeImpossibleSuccessors(instruction, varNum);
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitArrayStore(com.ibm.wala.ssa.SSAArrayStoreInstruction)
		 */
		@Override
		public void visitArrayStore(SSAArrayStoreInstruction instruction) {
			int varNum = instruction.getArrayRef();
			removeImpossibleSuccessors(instruction, varNum);
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitBinaryOp(com.ibm.wala.ssa.SSABinaryOpInstruction)
		 */
		@Override
		public void visitBinaryOp(SSABinaryOpInstruction instruction) {}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitCheckCast(com.ibm.wala.ssa.SSACheckCastInstruction)
		 */
		@Override
		public void visitCheckCast(SSACheckCastInstruction instruction) {}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitComparison(com.ibm.wala.ssa.SSAComparisonInstruction)
		 */
		@Override
		public void visitComparison(SSAComparisonInstruction instruction) {}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitConditionalBranch(com.ibm.wala.ssa.SSAConditionalBranchInstruction)
		 */
		@Override
		public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitConversion(com.ibm.wala.ssa.SSAConversionInstruction)
		 */
		@Override
		public void visitConversion(SSAConversionInstruction instruction) {}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitGet(com.ibm.wala.ssa.SSAGetInstruction)
		 */
		@Override
		public void visitGet(SSAGetInstruction instruction) {
			if (!instruction.isStatic()) {
				int varNum = instruction.getRef();
				removeImpossibleSuccessors(instruction, varNum);
			}
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitGetCaughtException(com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction)
		 */
		@Override
		public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitGoto(com.ibm.wala.ssa.SSAGotoInstruction)
		 */
		@Override
		public void visitGoto(SSAGotoInstruction instruction) {}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitInstanceof(com.ibm.wala.ssa.SSAInstanceofInstruction)
		 */
		@Override
		public void visitInstanceof(SSAInstanceofInstruction instruction) {}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitInvoke(com.ibm.wala.ssa.SSAInvokeInstruction)
		 */
		@Override
		public void visitInvoke(SSAInvokeInstruction instruction) {
			if (!instruction.isStatic()) {
				int varNum = instruction.getReceiver();
				removeImpossibleSuccessors(instruction, varNum);
			}
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitLoadMetadata(com.ibm.wala.ssa.SSALoadMetadataInstruction)
		 */
		@Override
		public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitMonitor(com.ibm.wala.ssa.SSAMonitorInstruction)
		 */
		@Override
		public void visitMonitor(SSAMonitorInstruction instruction) {
			int varNum = instruction.getRef();
			removeImpossibleSuccessors(instruction, varNum);
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitNew(com.ibm.wala.ssa.SSANewInstruction)
		 */
		@Override
		public void visitNew(SSANewInstruction instruction) {
			int varNum = instruction.getDef();
			removeImpossibleSuccessors(instruction, varNum);
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitPhi(com.ibm.wala.ssa.SSAPhiInstruction)
		 */
		@Override
		public void visitPhi(SSAPhiInstruction instruction) {}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitPi(com.ibm.wala.ssa.SSAPiInstruction)
		 */
		@Override
		public void visitPi(SSAPiInstruction instruction) {}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitPut(com.ibm.wala.ssa.SSAPutInstruction)
		 */
		@Override
		public void visitPut(SSAPutInstruction instruction) {
			if (!instruction.isStatic()) {
				int varNum = instruction.getRef();
				removeImpossibleSuccessors(instruction, varNum);
			}
		}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitReturn(com.ibm.wala.ssa.SSAReturnInstruction)
		 */
		@Override
		public void visitReturn(SSAReturnInstruction instruction) {}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitSwitch(com.ibm.wala.ssa.SSASwitchInstruction)
		 */
		@Override
		public void visitSwitch(SSASwitchInstruction instruction) {}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitThrow(com.ibm.wala.ssa.SSAThrowInstruction)
		 */
		@Override
		public void visitThrow(SSAThrowInstruction instruction) {}

		/* (non-Javadoc)
		 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitUnaryOp(com.ibm.wala.ssa.SSAUnaryOpInstruction)
		 */
		@Override
		public void visitUnaryOp(SSAUnaryOpInstruction instruction) {}

	}

}
