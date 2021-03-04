package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class LeakageComputation {

	private final List<Value> highInputs;
	private final Set<Variable> hVars;
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
	private Set<Variable> hVars() {
		Set<Variable> vars = new HashSet<>();
		highInputs.forEach(v -> Arrays.stream(v.getDeps()).forEach(f -> hVars.addAll(f.variables())));
		return vars;
	}

	public Formula createCountingFormula() throws UnexpectedTypeException {
		char[] binaryVal = LogicUtil.binaryRep(leakedValue.getVal(), leakedValue.getType());
		return IntStream.range(0, leakedValue.getDeps().length).mapToObj(i -> (binaryVal[i] == '1') ?
				leakedValue.getDepForBit(i) :
				LogicUtil.ff.not(leakedValue.getDepForBit(i))).reduce(LogicUtil.ff.constant(true), LogicUtil.ff::and);
	}
}
