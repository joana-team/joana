package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import com.google.common.collect.Lists;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

public class LeakageComputation {

	private final List<Value> highInputs;
	private List<Variable> hVars;
	private final Value leakedValue;

	public LeakageComputation(List<Value> highInputs, Value leakedValue) {
		this.highInputs = highInputs;
		this.leakedValue = leakedValue;
		this.hVars = hVars();

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
		Formula res =  IntStream.range(0, leakedValue.getDeps().length).mapToObj(i -> (binaryVal[i] == '1') ?
				leakedValue.getDepForBit(i) :
				LogicUtil.ff.not(leakedValue.getDepForBit(i)))
				.reduce(LogicUtil.ff.constant(true), LogicUtil.ff::and);
		System.out.println(res);
		return res;
	}

	public void compute() throws UnexpectedTypeException, IOException, InterruptedException {
		ApproxMC approxMC = new ApproxMC();
		approxMC.estimateModelCount(createCountingFormula(), hVars);
	}
}
