package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.State;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo.LoopSegment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Logger;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Variable;

import java.util.stream.Collectors;

public class SATAnalysis {

	private final Program program;
	private final Method entry;

	public SATAnalysis(Program program) {
		this.program = program;
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
		SATVisitor sv = new SATVisitor(this);

		computeSATDeps(m, sv);
		return sv.getVisitedInstructions();
	}

	public void computeSATDeps(Method m, SATVisitor sv) {
		initParameterDeps(m);
		initConstants(m);

		// implicit IF
		ImplicitIFVisitor imIFVisitor = new ImplicitIFVisitor();
		imIFVisitor.compute(m.getCFG());

		State state = State.init(program);
		state.toVisit.add(state.reentry);

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
			LoopSegment newSegment = new LoopSegment(l, state.currentSegment);
			state = newSegment.dynamic(state);

			if (b.hasRelevantCF()) {
				sv.visitBlock(m, b, -1);
				LoopHandler.analyze(m, b, this, l);
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

			for (BasicBlock succ : b.succs()) {
				State finalState = state;
				if (succ.isLoopHeader() || succ.preds().stream()
						.allMatch(pred -> finalState.visited.contains(pred.idx()))) {
					state.toVisit.add(succ);
				}
			}
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