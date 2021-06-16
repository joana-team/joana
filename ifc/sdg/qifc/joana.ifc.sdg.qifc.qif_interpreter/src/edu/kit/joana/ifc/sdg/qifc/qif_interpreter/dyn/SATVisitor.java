package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.ssa.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo.MethodSegment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.Environment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.stream.IntStream;

public class SATVisitor implements SSAInstruction.IVisitor {
	public static final String OUTPUT_FUNCTION = "edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out.print(I)V";

	private final Environment env;
	private final SATAnalysis SATAnalysis;
	private boolean containsOutOfScopeInstruction;
	private SSAInstruction outOfScopeInstruction;
	private BasicBlock block;
	private Method m;
	private int visitedInstructions;

	public BasicBlock getCurrentBlock() {
		return this.block;
	}

	public void visitBlock(Method m, BasicBlock b, int prevBlock) throws OutOfScopeException {
		this.block = b;
		this.m = m;

		for (SSAInstruction i : b.instructions()) {
			if (needsVisiting(i, b, m)) {
				visitedInstructions++;
				i.visit(this);
			}
		}

		if (containsOutOfScopeInstruction) {
			throw new OutOfScopeException(outOfScopeInstruction);
		}
	}

	private boolean needsVisiting(SSAInstruction i, BasicBlock b, Method m) {
		return (!i.hasDef() || m.getValue(i.getDef()).dynAnalysisNecessary(m)) && (
				!(i instanceof SSAConditionalBranchInstruction) || b.hasRelevantCF()) && (
				!(i instanceof SSAInvokeInstruction) || !((SSAInvokeInstruction) i).getDeclaredTarget().getSignature()
						.equals(OUTPUT_FUNCTION)) && (!(i instanceof SSAReturnInstruction) || !m.getProg()
				.getEntryMethod().equals(m)) && !(i instanceof SSAGotoInstruction);
	}

	public SATVisitor(SATAnalysis SATAnalysis, Environment env) {
		this.SATAnalysis = SATAnalysis;
		this.env = env;
		this.containsOutOfScopeInstruction = false;
		this.visitedInstructions = 0;
	}

	public int getVisitedInstructions() {
		return visitedInstructions;
	}

	@Override public void visitGoto(SSAGotoInstruction instruction) {
		// do nothing
	}

	@Override public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
		Array<? extends Value> array = (Array<? extends Value>) m.getValue(instruction.getArrayRef());
		visitArrayLoad(instruction, array);
	}

	public void visitArrayLoad(SSAArrayLoadInstruction instruction, Array<? extends Value> array) {
		Formula[] idx = m.getDepsForValue(instruction.getIndex());
		Formula[] res = array.currentlyAssigned(array.length() - 1);

		for (int i = 0; i < array.length() - 1; i++) {
			Formula[] idxSatArray = LogicUtil.asFormulaArray(LogicUtil.twosComplement(i, Type.INTEGER.bitwidth()));
			Formula assignmentCond = IntStream.range(0, Type.INTEGER.bitwidth())
					.mapToObj(j -> LogicUtil.ff.equivalence(idx[j], idxSatArray[j]))
					.reduce(LogicUtil.ff.constant(true), LogicUtil.ff::and);
			res = LogicUtil.ternaryOp(assignmentCond, array.currentlyAssigned(i), res);
		}
		m.setDepsForvalue(instruction.getDef(), res);
	}

	/*
	 * For each element of the referenced array:
	 * - check if the idx could be the same as the element idx
	 * - if yes: add assigned value as possible assignment
	 */
	@Override public void visitArrayStore(SSAArrayStoreInstruction instruction) {
		Array<? extends Value> array = (Array<? extends Value>) m.getValue(instruction.getArrayRef());

		if (array.isEffectivelyConstant(
				instruction)) { // all entries are constant after assignemnt according to pre-analysis
			IntStream.range(0, array.length()).forEach(i -> array
					.addAssignmentFromPreProcessing(this.block.generateImplicitFlowFormula(), i, instruction));
		} else if (m.getValue(instruction.getIndex()).isEffectivelyConstant(m) && array
				.isEffectivelyConstant(instruction, LogicUtil.numericValue(m.getDepsForValue(instruction
						.getIndex())))) { // idx amd assigned array element are constant according to pre-analysis
			array.addAssignmentFromPreProcessing(this.block.generateImplicitFlowFormula(),
					LogicUtil.numericValue(m.getDepsForValue(instruction.getIndex())), instruction);
		} else {
			visitArrayStore(instruction, array, block.generateImplicitFlowFormula());
		}
	}

	public void visitArrayStore(SSAArrayStoreInstruction instruction, Array<? extends Value> array, Formula implicitInfo) {
		Formula[] idx = m.getDepsForValue(instruction.getIndex());
		Formula[] assignedValue = m.getDepsForValue(instruction.getValue());

		for (int i = 0; i < array.length(); i++) {
			Formula[] idxSatArray = LogicUtil.asFormulaArray(LogicUtil.twosComplement(i, Type.INTEGER.bitwidth()));
			Formula assignmentCond = IntStream.range(0, Type.INTEGER.bitwidth())
					.mapToObj(j -> LogicUtil.ff.equivalence(idx[j], idxSatArray[j]))
					.reduce(LogicUtil.ff.constant(true), LogicUtil.ff::and);
			array.addAssignment(implicitInfo, i, assignmentCond, assignedValue);
		}
	}

	@Override public void visitComparison(SSAComparisonInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
		int op1ValNum = instruction.getUse(0);
		int op2ValNum = instruction.getUse(1);

		if (m.getValue(op1ValNum).getDeps() == null) {
			// if there doesn't exist a value object for this valueNumber at this point, it has to be constant
			SATAnalysis.createConstant(op1ValNum);
		}
		Formula[] op1 = m.getDepsForValue(op1ValNum);

		if (m.getValue(op2ValNum).getDeps() == null) {
			SATAnalysis.createConstant(op2ValNum);
		}
		Formula[] op2 = m.getDepsForValue(op2ValNum);

		IConditionalBranchInstruction.Operator operator = (IConditionalBranchInstruction.Operator) instruction
				.getOperator();

		Formula defForm;
		Formula[] diff = LogicUtil.sub(op1, op2);

		switch (operator) {
		case EQ:
			defForm = LogicUtil.equalsZero(diff);
			break;
		case NE:
			defForm = LogicUtil.ff.not(LogicUtil.equalsZero(diff));
			break;
		case LT:
			defForm = LogicUtil.ff.equivalence(LogicUtil.ff.constant(true), diff[0]);
			break;
		case GE:
			defForm = LogicUtil.ff.equivalence(LogicUtil.ff.constant(false), diff[0]);
			break;
		case GT:
			defForm = LogicUtil.ff.and(LogicUtil.ff.not(LogicUtil.equalsZero(diff)),
					LogicUtil.ff.equivalence(LogicUtil.ff.constant(false), diff[0]));
			break;
		case LE:
			defForm = LogicUtil.ff
					.or(LogicUtil.equalsZero(diff), LogicUtil.ff.equivalence(LogicUtil.ff.constant(true), diff[0]));
			break;
		default:
			throw new IllegalStateException("Unexpected value: " + operator);
		}
		block.setCondExpr(defForm);
	}

	@Override public void visitSwitch(SSASwitchInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitReturn(SSAReturnInstruction instruction) {
		if (!instruction.returnsVoid()) {
			m.getReturn().addReturnSite(instruction, block);
		}
	}

	@Override public void visitGet(SSAGetInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitPut(SSAPutInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitInvoke(SSAInvokeInstruction instruction) {
		if (!instruction.getCallSite().getDeclaredTarget().getSignature().equals(OUTPUT_FUNCTION)) {

			String calleeId = instruction.getDeclaredTarget().getSignature();
			IInvocationHandler handler;
			if (!m.getProg().getMethod(calleeId).isDepsAnalyzed()) {
				if (m.getProg().isRecursive(instruction.getDeclaredTarget(), m.getCg())) {
					handler = new RecursiveFunctionInvocationHandler();
				} else {
					handler = new InvocationHandler();
				}
				handler.analyze(m.getProg().getMethod(calleeId), env);
			}

			if (instruction.getNumberOfDefs() > 0) {
				Method callee = m.getProg().getMethod(instruction.getDeclaredTarget().getSignature());
				MethodSegment seg = callee.getSegments().get(instruction);
				Type returnType = callee.getReturnType();

				if (seg.dynAnaFeasible) {
					setReturnValueDeps(callee, returnType, instruction);
				} else {
					setReturnValueVars(callee, returnType, instruction);
				}
			}
		}
	}

	private void setReturnValueDeps(Method callee, Type returnType, SSAInvokeInstruction instruction) {
		if (returnType.equals(Type.ARRAY)) {
			try {
				Array arr = Array.newArray(callee.getReturnElementType(), instruction.getDef(), false);
				arr.setValueDependencies(
						((IReturnValue<Formula[][]>) callee.getReturn()).getReturnValueForCallSite(instruction, m));
				m.addValue(instruction.getDef(), arr);
			} catch (UnexpectedTypeException e) {
				e.printStackTrace();
			}

		} else {
			m.setDepsForvalue(instruction.getDef(), (Formula[]) callee.getReturnValueForCall(instruction, m));
		}
	}

	private void setReturnValueVars(Method callee, Type returnType, SSAInvokeInstruction instruction) {
		if (returnType.equals(Type.ARRAY)) {
			try {
				Array arr = Array.newArray(callee.getReturnElementType(), instruction.getDef(), true);
				m.addValue(instruction.getDef(), arr);
			} catch (UnexpectedTypeException e) {
				e.printStackTrace();
			}
		} else {
			m.setDepsForvalue(instruction.getDef(), LogicUtil.createVars(instruction.getDef(), returnType.bitwidth()));
		}
	}

	@Override public void visitNew(SSANewInstruction instruction) {
		assert (m.hasValue(instruction.getDef()));
	}

	@Override public void visitArrayLength(SSAArrayLengthInstruction instruction) {
		Formula[] length = LogicUtil.asFormulaArray(LogicUtil
				.twosComplement(((Array<? extends Value>) m.getValue(instruction.getArrayRef())).length(),
						Type.INTEGER.bitwidth()));
		edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value defVal;
		m.setDepsForvalue(instruction.getDef(), length);
	}

	@Override public void visitThrow(SSAThrowInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitMonitor(SSAMonitorInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitCheckCast(SSACheckCastInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitInstanceof(SSAInstanceofInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	/**
	 * Use fresh variables for the defined value, bc depending on the program, the values that this instruction depends on may not have been evaluated yet
	 * Remember to add equality constraints later on!
	 *
	 * @param instruction a phi instruction
	 */
	@Override public void visitPhi(SSAPhiInstruction instruction) {
		if (!block.isLoopHeader()) {
			if (block.preds().stream().map(pred -> (pred.isDummy() ? pred.preds().get(0) : pred))
					.anyMatch(BasicBlock::isLoopHeader)) {
				handleBreakPhi(instruction);
			} else {
				handleCondPhi(instruction);
			}
		} else {
			handleLoopPhi(instruction);
		}
	}

	// if the out-of-loop successor of a loop-header contains phi-instructions, these are the result of a break statement in the loop-body
	// the actual value of this def depends on 2 conditions: the loop-head and the break-condition
	private void handleBreakPhi(SSAPhiInstruction instruction) {
		BasicBlock loopHeader = block.preds().stream().map(pred -> (pred.isDummy()) ? pred.preds().get(0) : pred)
				.filter(BasicBlock::isLoopHeader).findFirst().get();
		LoopBody l = m.getLoops().stream().filter(loop -> loop.getHead().equals(loopHeader)).findFirst().get();
		int normalExitValueIdx = (loopHeader.ownsValue(instruction.getUse(0))) ? 0 : 1;
		int normalExitValue = instruction.getUse(normalExitValueIdx);
		int breakExitValue = instruction.getUse(1 - normalExitValueIdx);

		// finding the block that ends w/ the break:
		// predecessors of the current block (containing the phi-instruction resulting from the break statement) are a dummy node (coming from the loop header) and a non-dummy block (coming from the goto-block out of the loop)
		// the break-block is then the first non-dummy predecessor of the goto-block
		BasicBlock gotoBlock = block.preds().stream().filter(pred -> !pred.isDummy()).findFirst().get();

		Formula[] breakDeps = LoopHandler
				.computeBreakValues(l, instruction.getDef(), normalExitValue, breakExitValue, gotoBlock);
		m.setDepsForvalue(instruction.getDef(), breakDeps);
	}

	/*
	assign fresh variables to the def value
	These are used as "input variables" for the in-loop computations and later substituted by the actual value formulas depending on the current iteration
	 */
	private void handleLoopPhi(SSAPhiInstruction instruction) {
		Value defVal = m.getValue(instruction.getDef());
		Variable[] vars = LogicUtil.createVars(defVal.getValNum(), defVal.getType().bitwidth());
		m.setDepsForvalue(instruction.getDef(), vars);
		m.addVarsToValue(instruction.getDef(), vars);
	}

	private void handleCondPhi(SSAPhiInstruction instruction) {
		Formula[] op1 = m.getDepsForValue(instruction.getUse(0));
		Formula[] op2 = m.getDepsForValue(instruction.getUse(1));

		BasicBlock predZero = block.preds().get(0);
		m.setDepsForvalue(instruction.getDef(), LogicUtil.ternaryOp(predZero.generateImplicitFlowFormula(), op1, op2));
	}

	@Override public void visitPi(SSAPiInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

	@Override public void visitBinaryOp(SSABinaryOpInstruction instruction) {
		Formula[] op1 = m.getDepsForValue(instruction.getUse(0));
		Formula[] op2 = m.getDepsForValue(instruction.getUse(1));

		int def = instruction.getDef();

		IBinaryOpInstruction.Operator operator = (IBinaryOpInstruction.Operator) instruction.getOperator();
		Formula[] defForm = null;
		switch (operator) {
		case SUB:
			if (LogicUtil.isConstant(op2) && LogicUtil.numericValue(op2) == 0) {
				defForm = op1;
			} else {
				defForm = LogicUtil.sub(op1, op2);
			}
			break;
		case ADD:
			if (LogicUtil.isConstant(op1) && LogicUtil.numericValue(op1) == 0) {
				defForm = op2;
			} else if (LogicUtil.isConstant(op2) && LogicUtil.numericValue(op2) == 0) {
				defForm = op1;
			} else {
				defForm = LogicUtil.add(op1, op2);
			}
			break;
		case MUL:
			if (LogicUtil.isConstant(op1) && LogicUtil.numericValue(op1) == 1) {
				defForm = op2;
			} else if (LogicUtil.isConstant(op2) && LogicUtil.numericValue(op2) == 1) {
				defForm = op1;
			} else {
				defForm = LogicUtil.mult(op1, op2);
			}
			break;
		case DIV:
		case REM:
			break;
		case AND:
			defForm = LogicUtil.and(op1, op2);
			break;
		case OR:
			defForm = LogicUtil.or(op1, op2);
			break;
		case XOR:
			defForm = LogicUtil.xor(op1, op2);
			break;
		}
		assert defForm != null;
		m.setDepsForvalue(def, defForm);
	}

	@Override public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
		int def = instruction.getDef();
		int opValNum = instruction.getUse(0);

		Formula[] op = m.getDepsForValue(opValNum);

		IUnaryOpInstruction.Operator operator = (IUnaryOpInstruction.Operator) instruction.getOpcode();

		if (operator == IUnaryOpInstruction.Operator.NEG) {
			m.setDepsForvalue(def, LogicUtil.neg(op));
		}
	}

	@Override public void visitConversion(SSAConversionInstruction instruction) {
		containsOutOfScopeInstruction = true;
		outOfScopeInstruction = instruction;
	}

}