package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.datastructures.Substitution;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

	public void initParameterDeps(Method m) {
		// create literals for method parameters
		int[] params = m.getIr().getParameterValueNumbers();
		for (int i = 1; i < params.length; i++) {
			m.setDepsForvalue(params[i], LogicUtil.createVars(params[i], m.getParamType(i)));
		}
	}

	public void initConstants(Method m) {
		// initialize formula arrays for all constant values
		m.getProgramValues().values().stream().filter(Value::isConstant).forEach(c -> {
			try {
				c.setDeps(LogicUtil.asFormulaArray(LogicUtil.binaryRep(c.getVal(), c.getType())));
			} catch (UnexpectedTypeException e) {
				e.printStackTrace();
			}
		});
	}

	public void computeSATDeps(Method m) {
		initParameterDeps(m);
		initConstants(m);


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
		SimplePhiVisitor v = new SimplePhiVisitor(m);
		Map<Integer, Formula[]> subs = v.computePhiDeps();

		Substitution substitution = new Substitution();
		for(Integer valNum: subs.keySet()) {
			Value value = m.getValue(valNum);
			assert(Arrays.stream(value.getDeps()).allMatch(Formula::isAtomicFormula));
			for (int i = 0; i < value.getWidth(); i++) {
				substitution.addMapping((Variable) value.getDepForBit(i), subs.get(valNum)[i]);
			}
		}

		// substitute phi variables in leaked value
		for (Value val: m.getProgramValues().values().stream().filter(Value::isLeaked).collect(Collectors.toList())) {
			for (int i = 0; i < val.getWidth(); i++) {
				val.getDeps()[i] = val.getDepForBit(i).substitute(substitution);
			}
		}
	}

	public void createConstant(int op1) {
		Int constant = (Int) entry.getValue(op1);
		assert constant != null;
		constant.setDeps(
				LogicUtil.asFormulaArray(LogicUtil.twosComplement((Integer) constant.getVal(), constant.getWidth())));
	}
}
