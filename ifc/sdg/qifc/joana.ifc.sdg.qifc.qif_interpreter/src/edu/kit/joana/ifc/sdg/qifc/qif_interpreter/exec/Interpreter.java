package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.exec;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.ssa.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ParameterException;

import java.io.PrintStream;
import java.util.List;

public class Interpreter {

	private final Program program;
	private final PrintStream out;

	public Interpreter(Program p, PrintStream out) {
		this.program = p;
		this.out = out;
	}

	public Interpreter(Program p, List<String> args) {
		this(p, System.out);
	}

	public boolean execute(List<String> args) throws ParameterException {

		if(!applyArgs(args, program, program.getEntryMethod())) {
			throw new ParameterException("Wrong input parameter for function.");
		}

		ExecutionVisitor ev = new ExecutionVisitor();
		ev.executeFrom(program.getEntryMethod().getCFG().entry());
		return true;
	}

	public boolean applyArgs(List<String> args, Program p, Method m) {
		if (!(m.getCg().getMethod().getNumberOfParameters() - 1 == args.size())) {
			return false;
		}

		// i refers to the position of the input arguments in the args array
		// in the program, the first parameter of every function is the 'this' reference
		// hence we need to access the parameters w/ i + 1
		for(int i = 0; i < args.size(); i++) {
			int paramNum = i + 1;
			Object paramVal;
			switch(m.getParamType(paramNum)) {
			case INTEGER:
				try {
					paramVal = Integer.parseInt(args.get(i));
				} catch (NumberFormatException e) {
					return false;
				}
				break;
			case CUSTOM:
			default:
				throw new IllegalStateException("Unexpected value: " + m.getParamType(paramNum));
			}

			int valNum = m.getIr().getParameter(paramNum);
			Value param = p.getOrCreateValue(valNum, m.getParamType(paramNum), m);
			param.setVal(paramVal);
		}
		return true;
	}

	public class ExecutionVisitor extends SSAInstruction.Visitor {

		private static final String OUTPUT_FUNCTION = "edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out.print(I)V";

		int currBlockIdx;
		int prevBlockIdx;
		BBlock block;

		public void executeFrom(BBlock start) {
			currBlockIdx = start.getWalaBasicBLock().getNumber();
			block = start;
			for(SSAInstruction i: start.instructions()) {
				i.visit(this);
			}

			if (!block.isCondHeader() && !block.isLoopHeader() &&!start.succs().isEmpty()) {
				prevBlockIdx = currBlockIdx;
				executeFrom(start.succs().get(0));
			}
		}

		@Override public void visitGoto(SSAGotoInstruction instruction) {

		}

		@Override public void visitBinaryOp(SSABinaryOpInstruction instruction) {
			Integer op1 = (Integer) program.getOrCreateValue(instruction.getUse(0),
					Type.INTEGER,
					block.getCFG().getMethod()).getVal();
			Integer op2 = (Integer) program.getOrCreateValue(instruction.getUse(1),
					Type.INTEGER,
					block.getCFG().getMethod()).getVal();
			IBinaryOpInstruction.Operator operator = (IBinaryOpInstruction.Operator) instruction.getOperator();

			int def;
			switch (operator) {
			case ADD:
				def = op1 + op2;
				break;
			case SUB:
				def = op1 - op2;
				break;
			case MUL:
				def = op1 * op2;
				break;
			case DIV:
				def = op1 / op2;
				break;
			case REM:
				def = op1 % op2;
				break;
			case AND:
				def = op1 & op2;
				break;
			case OR:
				def = op1 | op2;
				break;
			case XOR:
				def = op1 ^ op2;
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + operator);
			}
			program.setValue(instruction.getDef(), def);
		}

		@Override public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
			int use = (Integer) program.getOrCreateValue(instruction.getUse(0),
					Type.INTEGER,
					block.getCFG().getMethod()).getVal();
			if (!instruction.getOpcode().equals(IUnaryOpInstruction.Operator.NEG)) {
				throw new IllegalStateException("Unexpected value: " + IUnaryOpInstruction.Operator.NEG);
			} else {
				program.setValue(instruction.getDef(), -use);
			}
		}

		@Override public void visitComparison(SSAComparisonInstruction instruction) {

		}

		@Override public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {

		}

		@Override public void visitReturn(SSAReturnInstruction instruction) {

		}

		@Override public void visitPhi(SSAPhiInstruction instruction) {

		}

		@Override public void visitPi(SSAPiInstruction instruction) {

		}

		@Override
		public void visitInvoke(SSAInvokeInstruction instruction) {

			if (instruction.getCallSite().getDeclaredTarget().getSignature().equals(OUTPUT_FUNCTION)) {
				out.println(program.getOrCreateValue(instruction.getUse(0), Type.INTEGER, block.getCFG().getMethod()).getVal());
			}
		}
	}
}
