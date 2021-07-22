package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.State;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.Environment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Logger;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.stream.Collectors;

public class SATAnalysis {

	private Environment env;
	private final Program program;
	private final Method entry;

	public SATAnalysis(Environment env) {
		this.env = env;
		this.program = env.iProgram;
		this.entry = program.getEntryMethod();
	}

	public void computeSATDeps() {
		long start_time = System.currentTimeMillis();
		int visitedInstructions = computeSATDeps(this.entry);
		long duration = System.currentTimeMillis() - start_time;
		Logger.satAnalysis(visitedInstructions, duration);
		this.entry.finishedAnalysis();
	}

	public void initParameterDeps(Method m) {
		// create literals for method parameters
		int[] params = m.getIr().getParameterValueNumbers();
		for (int i = 1; i < params.length; i++) {
			if (m.getParamType(i).equals(Type.ARRAY))
				continue; // already initialized
			Variable[] vars = LogicUtil.createVars(params[i], m.getParamType(i).bitwidth());
			m.setDepsForvalue(params[i], vars);
			m.addVarsToValue(params[i], vars);
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

	public int computeSATDeps(Method m) {
		SATVisitor sv = new SATVisitor(this, env);

		computeSATDeps(m, sv);
		return sv.getVisitedInstructions();
	}

	public void computeSATDeps(Method m, SATVisitor sv) {
		initParameterDeps(m);
		initConstants(m);

		// implicit IF
		ImplicitIFVisitor imIFVisitor = new ImplicitIFVisitor();
		imIFVisitor.compute(m.getCFG());

		State state = State.init(env, m);
		state.toVisit.add(state.current);

		try {
			computeSATDeps(state, m, sv);
		} catch (OutOfScopeException e) {
			e.printStackTrace();
		}
	}

	public void computeSATDeps(State state, Method m, SATVisitor sv) throws OutOfScopeException {
		if (state.toVisit.isEmpty()) {
			return;
		}
		BasicBlock b = state.toVisit.remove(0);
		state.visited.add(b.idx());

		if (b.isLoopHeader()) {
			LoopBody l = m.getLoops().stream().filter(loop -> loop.getHead().idx() == b.idx()).findFirst().get();

			if (b.hasRelevantCF()) {
				sv.visitBlock(m, b, -1);
				if (l.getSegment().dynAnaFeasible || !this.env.args.onlyStatic) {
					LoopHandler.analyze(m, b, this, l, env);
					for (SSAInstruction i : b.instructions()) {
						if (i instanceof SSAPhiInstruction) {
							// see if we can find any constant bits that were undetected by nildumu
							Formula[] op1 = m.getDepsForValue(i.getUse(0));
							Formula[] op2 = m.getDepsForValue(i.getUse(1));

							for (int j = 0; j < op1.length; j++) {
								if (op1[j].isConstantFormula() && op2[j].isConstantFormula() && op1[j].equals(op2[j])) {
									m.getValue(i.getDef()).setDepforBit(j, op1[j]);
								}
							}
						}
					}
				}
				for (SSAInstruction i : b.instructions()) {
					if (i instanceof SSAPhiInstruction) {
						m.setDepsForvalue(i.getDef(), m.getValue(i.getDef()).getMaskedDeps());
					}
				}
			}

			// add all after-loop successors, but skip the dummy blocks
			for (BasicBlock succ : b.succs().stream().filter(succ -> !l.getBlocks().contains(succ))
					.collect(Collectors.toList())) {
				if (succ.isDummy()) {
					succ = succ.succs().get(0);
				}
				state.toVisit.add(succ);
			}
		} else {

			sv.visitBlock(m, b, -1);
		}
		computeSATDeps(state, m, sv);
	}

	public void createConstant(int op1) {
		Int constant = (Int) entry.getValue(op1);
		assert constant != null;
		constant.setDeps(
				LogicUtil.asFormulaArray(LogicUtil.twosComplement((Integer) constant.getVal(), constant.getWidth())));
	}
}