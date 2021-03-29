package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;
import org.logicng.io.writers.FormulaDotFileWriter;

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
		res = addPhiDepsRec(res, -1);
		return res;
	}

	private Formula addPhiDepsRec(Formula lastAdded, int lastSub) {
		List<Formula> toAdd = new ArrayList<>();
		for (int i : m.getPhiValPossibilities().keySet()) {
			Formula[] vars = m.getDepsForValue(i);
			if (i != lastSub && Arrays.stream(vars).anyMatch(var -> lastAdded.containsVariable((Variable) var))) {
				List<Formula> possibilities = new ArrayList<>();
				for (Pair<Formula[], Formula> p : m.getPhiValPossibilities().get(i)) {
					Formula reduced = IntStream.range(0, vars.length)
							.mapToObj(k -> LogicUtil.ff.equivalence(vars[k], p.fst[k]))
							.reduce(p.snd, LogicUtil.ff::and);
					possibilities.add(addPhiDepsRec(reduced, i));
				}
				toAdd.add(possibilities.stream().reduce(LogicUtil.ff.constant(false), LogicUtil.ff::or));
			}
		}
		return toAdd.stream().reduce(lastAdded, LogicUtil.ff::and);
	}

	public void compute(String outputDirectory) throws UnexpectedTypeException, IOException {
		Formula count = createCountingFormula();
		// System.out.println("formula: " + count);
		FormulaDotFileWriter.write("dot.dot", count, true);
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
