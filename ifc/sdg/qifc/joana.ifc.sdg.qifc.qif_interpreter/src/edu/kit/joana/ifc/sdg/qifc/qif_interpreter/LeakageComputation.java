package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

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
import java.util.stream.Collectors;
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
		List<Formula> reducedPhiDeps = m.getPhiDeps().values().stream()
				.map(arr -> Arrays.stream(arr).reduce(LogicUtil.ff.constant(true), LogicUtil.ff::and))
				.collect(Collectors.toList());
		res = reducedPhiDeps.stream().reduce(res, LogicUtil.ff::and);
		System.out.println(res);
		return res;
	}

	public void compute() throws UnexpectedTypeException, IOException, InterruptedException {
		ApproxMC approxMC = new ApproxMC();
		approxMC.estimateModelCount(createCountingFormula(), hVars);
	}
}
