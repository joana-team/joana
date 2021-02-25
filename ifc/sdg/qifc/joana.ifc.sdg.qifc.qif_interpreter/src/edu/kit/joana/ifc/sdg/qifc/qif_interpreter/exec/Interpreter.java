package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.exec;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.MissingValueException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
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

	public Interpreter(Program p) {
		this(p, System.out);
	}


	public boolean execute(List<String> args) throws ParameterException, OutOfScopeException {

		System.out.println("----------- Starting program execution -----------");

		if (!applyArgs(args, program, program.getEntryMethod())) {
			throw new ParameterException("Wrong input parameter for program.");
		}

		ExecutionVisitor ev = new ExecutionVisitor(program.getEntryMethod(), this.out);
		executeMethod(program.getEntryMethod(), args);

		return true;
	}

	/**
	 * @param m the method to execute
	 * @param args input parameters for the method
	 * @throws OutOfScopeException if the method contains an instruction that is not implemented for this interpreter
	 */
	public void executeMethod(Method m, List<String> args) throws OutOfScopeException {
		ExecutionVisitor ev = new ExecutionVisitor(m);

		int prevBlock = -1;
		int currentBlock = program.getEntryMethod().getCFG().entry().idx();
		int nextBlock = ev.executeBlock(program.getEntryMethod().getCFG().entry(), prevBlock);
		assert(nextBlock >= 0);

		while (nextBlock != -1) {
			prevBlock = currentBlock;
			currentBlock = nextBlock;
			BBlock next = BBlock.getBlockForIdx(currentBlock);
			nextBlock = ev.executeBlock(next, prevBlock);

			// skip dummy Blocks
			if (nextBlock < -1) {
				BBlock dummy = BBlock.getBlockForIdx(nextBlock);
				nextBlock = dummy.succs().get(0).idx();
			}
		}
		m.setReturnValue(ev.getReturnValue());
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
			Value param = m.getValue(valNum);
			param.setVal(paramVal);
		}
		return true;
	}
}
