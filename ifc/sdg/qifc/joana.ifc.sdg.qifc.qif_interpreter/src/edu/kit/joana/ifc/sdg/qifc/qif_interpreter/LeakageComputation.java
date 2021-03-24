package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class LeakageComputation {

	private final List<Value> highInputs;
	private List<Variable> hVars;
	private final Value leakedValue;
	private final Method m;

	public LeakageComputation(List<Value> highInputs, Value leakedValue, Method m) {
		this.highInputs = highInputs;
		this.leakedValue = leakedValue;
		this.hVars = hVars();
		this.m = m;

		// all values are statically known except the secret inputs. So our formulas should only depend on those
		// assert(Arrays.stream(leakedValue.getDeps()).allMatch(f -> hVars.containsAll(f.variables())));
		assert (leakedValue.assigned());
	}

	// extract variables from all high inputs
	private List<Variable> hVars() {
		List<Variable> vars = new ArrayList<>();
		highInputs.forEach(v -> Arrays.stream(v.getDeps()).forEach(f -> vars.addAll(f.variables())));
		return vars;
	}

	public Formula createCountingFormula() throws UnexpectedTypeException {
		char[] binaryVal = LogicUtil.binaryRep(leakedValue.getVal(), leakedValue.getType());
		Formula res = IntStream.range(0, leakedValue.getDeps().length).mapToObj(i -> (binaryVal[i] == '1') ?
				leakedValue.getDepForBit(i) :
				LogicUtil.ff.not(leakedValue.getDepForBit(i))).reduce(LogicUtil.ff.constant(true), LogicUtil.ff::and);

		// add equivalences describing the control flow at phi nodes
		List<Formula> reducedPhiDeps = new ArrayList<>();

		for (Integer i : m.getPhiValPossibilities().keySet()) {
			List<Pair<Formula[], Formula>> possibilities = m.getPossibleValues(i);
			Formula[] vars = m.getDepsForValue(i);
			Formula asSingleFormula = LogicUtil.ff.constant(false);
			for (Pair<Formula[], Formula> p : possibilities) {
				asSingleFormula = LogicUtil.ff.or(asSingleFormula,
						IntStream.range(0, vars.length).mapToObj(j -> LogicUtil.ff.equivalence(vars[j], p.fst[j]))
								.reduce(p.snd, LogicUtil.ff::and));
			}
			reducedPhiDeps.add(asSingleFormula);
		}

		res = reducedPhiDeps.stream().reduce(res, LogicUtil.ff::and);
		// System.out.println(res);
		return res;
	}

	public void compute(String outputDirectory) throws UnexpectedTypeException, IOException {

		Formula count = createCountingFormula();
		int modelCount;

		if (hVars.stream().noneMatch(count::containsVariable)) {
			modelCount = (int) Math.pow(2, hVars.size());
		} else {
			ApproxMC approxMC = new ApproxMC(outputDirectory);
			modelCount = approxMC.estimateModelCount(count, hVars);
		}
		System.out.println("# of inputs w/ the same output: " + modelCount);
	}
}
