package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ConversionException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.Slicer;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu.Converter;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu.NildumuOptions;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu.NildumuProgram;
import nildumu.Lattices;
import nildumu.Parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaticPreprocessingStage implements IStage {

	NildumuOptions options = NildumuOptions.DEFAULT;
	private boolean success = false;

	@Override public Environment execute(Environment env) {

		assert (env.completedStage(Stage.BUILD));
		Map<Integer, Boolean> neededDefs = new HashMap<>();

		// ---------------------- Nildumu ------------------------
		Converter c = new Converter();
		Parser.ProgramNode p = null;
		try {
			p = c.convertProgram(env.iProgram);
		} catch (ConversionException e) {
			e.printStackTrace();
		}

		assert p != null;
		env.nProgram = new NildumuProgram(p, options);
		for (Map.Entry<Integer, Value> e : env.iProgram.getEntryMethod().getProgramValues().entrySet()) {
			Value.BitLatticeValue[] bitMask = createBitMask(e.getValue(), env.nProgram.context
					.getVariableValue(Converter.varName(e.getKey(), env.iProgram.getEntryMethod())));
			e.getValue().setConstantBitMask(bitMask);
		}

		// ------------------- Program Slice ----------------------
		Slicer slicer = new Slicer();
		List<Integer> leakedVals = env.iProgram.getEntryMethod().getLeakedValues();

		for (int leaked : leakedVals) {
			updateNeededDefs(neededDefs, slicer.findNeededDefs(leaked, env.iProgram.getEntryMethod()));
		}

		env.iProgram.getEntryMethod().getProgramValues()
				.forEach((key, value) -> value.setInfluencesLeak(neededDefs.getOrDefault(key, true)));

		success = true;
		return env;
	}

	private void updateNeededDefs(Map<Integer, Boolean> neededDefs, Map<Integer, Boolean> update) {
		for (int key : update.keySet()) {
			neededDefs.put(key, neededDefs.getOrDefault(key, false) || update.get(key));
		}
	}

	private Value.BitLatticeValue[] createBitMask(Value v, Lattices.Value latticeValue) {
		if (v.getWidth() != latticeValue.bits.size()) {
			assert (latticeValue.toString().equals("xx"));
			return Collections.nCopies(v.getWidth(), Value.BitLatticeValue.UNKNOWN)
					.toArray(new Value.BitLatticeValue[0]);
		}

		Value.BitLatticeValue[] mask = new Value.BitLatticeValue[latticeValue.bits.size()];
		for (int i = 0; i < mask.length; i++) {
			switch (latticeValue.bits.get(mask.length - i - 1).val()) {
			case ZERO:
				mask[i] = Value.BitLatticeValue.ZERO;
				break;
			case ONE:
				mask[i] = Value.BitLatticeValue.ONE;
				break;
			case S:
			case N:
			case E:
			case X:
			case U:
				mask[i] = Value.BitLatticeValue.UNKNOWN;
			}
		}
		return mask;
	}

	@Override public boolean success() {
		return success;
	}

	@Override public Stage identity() {
		return Stage.STATIC_PREPROCESSING;
	}
}