package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;

import java.util.Arrays;

public class StaticAnalysis {

	private final Program program;
	private final Method entry;

	public StaticAnalysis(Program program) {
		this.program = program;
		this.entry = program.getEntryMethod();
	}

	public void computeSATDeps() {
		computeSATDeps(this.entry);
	}

	public void computeSATDeps(Method m) {
		// create literals for method parameters
		int[] params = m.getIr().getParameterValueNumbers();
		for (int i = 1; i < params.length; i++) {
			m.setDepsForvalue(params[i], LogicUtil.createVars(params[i], m.getParamType(i)));
		}

		// initialize formula arrays for all constant values
		m.getProgramValues().values().stream().filter(Value::isConstant).forEach(c -> {
			try {
				c.setDeps(LogicUtil.asFormulaArray(LogicUtil.binaryRep(c.getVal(), c.getType())));
			} catch (UnexpectedTypeException e) {
				e.printStackTrace();
			}
		});

		// implicit IF
		ImplicitIFVisitor imIFVisitor = new ImplicitIFVisitor();
		imIFVisitor.compute(m.getCFG());

		// explicit IF
		SATVisitor sv = new SATVisitor(this);

		for (BBlock bBlock : m.getCFG().getBlocks()) {
			try {
				sv.visitBlock(m, bBlock, -1);
			} catch (OutOfScopeException e) {
				e.printStackTrace();
			}
		}

		// handle Phi's
		SimplePhiVisitor spv = new SimplePhiVisitor(m);
		spv.computePhiDeps();

		for (Value val : m.getProgramValues().values()) {
			System.out.println(String.format("Valnum %d -- Value: %d", val.getValNum(), val.getVal()));
			Arrays.stream(val.getDeps()).forEach(ff -> System.out.println(ff.cnf()));
		}
	}

	public void createConstant(int op1) {
		Int constant = (Int) entry.getValue(op1);
		assert constant != null;
		constant.setDeps(
				LogicUtil.asFormulaArray(LogicUtil.twosComplement((Integer) constant.getVal(), constant.getWidth())));
	}
}
