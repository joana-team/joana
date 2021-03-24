package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;

import java.util.*;
import java.util.stream.Collectors;

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
			m.setDepsForvalue(params[i], LogicUtil.createVars(params[i], m.getParamType(i).bitwidth()));
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

		Queue<BBlock> toVisit = new ArrayDeque<>();
		toVisit.add(m.getCFG().getBlock(0));

		while(!toVisit.isEmpty()) {
			BBlock b = toVisit.poll();

			if (b.isLoopHeader()) {
				LoopBody l = SimpleLoopHandler.analyze(m, b, sv);
				m.addLoop(l);
				toVisit.addAll(b.succs().stream().filter(succ -> !l.getBlocks().contains(succ)).collect(Collectors.toList()));
			} else {
				try {
					sv.visitBlock(m, b, -1);
					toVisit.addAll(b.succs());
				} catch (OutOfScopeException e) {
					e.printStackTrace();
				}
			}
		}

		// simulate loops up to 3 iterations
		for (LoopBody l : m.getLoops()) {
			l.getRun(3);
		}

		// handle Phi's
		SimplePhiVisitor v = new SimplePhiVisitor(m);
		v.computePhiDeps();

		// -------------- print Phi results -------------
		System.out.println("Phi results: ");
		Map<Integer, List<Pair<Formula[], Formula>>> phiRes = m.getPhiValPossibilities();

		for (int i : phiRes.keySet()) {
			System.out.println(i);
			phiRes.get(i).forEach(p -> System.out.println("Val: " + Arrays.toString(p.fst) + "\nImplicit: " + p.snd));
		}
		// -----------------------------------------------
	}

	public void createConstant(int op1) {
		Int constant = (Int) entry.getValue(op1);
		assert constant != null;
		constant.setDeps(
				LogicUtil.asFormulaArray(LogicUtil.twosComplement((Integer) constant.getVal(), constant.getWidth())));
	}
}
