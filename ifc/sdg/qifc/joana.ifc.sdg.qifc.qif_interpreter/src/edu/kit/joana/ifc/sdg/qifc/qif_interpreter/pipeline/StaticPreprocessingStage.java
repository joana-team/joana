package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Array;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
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
		Map<Pair<Method, Integer>, Boolean> neededDefs = new HashMap<>();
		Map<Pair<Method, Integer>, Boolean> neededCF = new HashMap<>();

		// ---------------------- Nildumu ------------------------
		Converter c = new Converter();
		Parser.ProgramNode p = null;
		try {
			p = c.convertProgram(env.iProgram);
			// System.out.println(p.toPrettyString());
		} catch (ConversionException e) {
			e.printStackTrace();
		}

		assert p != null;
		env.nProgram = new NildumuProgram(p, options, Converter.loopMethods, Converter.methods);

		for (Method m : env.iProgram.getMethods()) {
			for (Map.Entry<Integer, Value> e : m.getProgramValues().entrySet()) {

				if (e.getValue().isArrayType()) {
					for (Map.Entry<Pair<Method, SSAArrayStoreInstruction>, Integer> i : Converter.arrayVarIndices
							.entrySet()) {
						if (!i.getKey().fst.equals(m))
							continue;
						List<String> varNames = Converter.arrayVarName(i.getKey().snd.getArrayRef(), m, i.getValue());
						int elementWidth = ((Array<? extends Value>) e.getValue()).elementType().bitwidth();
						Value.BitLatticeValue[][] bitMask = varNames.stream().map(varName -> createBitMask(elementWidth,
								env.nProgram.context.getVariableValue(varName)))
								.toArray(Value.BitLatticeValue[][]::new);
						m.getArray(i.getKey().snd.getArrayRef()).addConstantBitMask(i.getKey().snd, bitMask);
					}
				} else {
					Value.BitLatticeValue[] bitMask = createBitMask(e.getValue().getWidth(),
							env.nProgram.context.getVariableValue(Converter.varName(e.getKey(), m)));
					e.getValue().setConstantBitMask(bitMask);
				}
			}
		}

		// ------------------- Program Slice ----------------------
		Slicer slicer = new Slicer();
		List<Integer> leakedVals = env.iProgram.getEntryMethod().getLeakedValues();

		for (int leaked : leakedVals) {
			slicer.findRelevantSlice(leaked, env.iProgram.getEntryMethod());
			updateNeededDefs(neededDefs, slicer.neededDefs);
			updateNeededDefs(neededCF, slicer.neededCF);
		}

		for (Method m : env.iProgram.getMethods()) {
			m.getProgramValues()
					.forEach((key, value) -> value.setInfluencesLeak(neededDefs.getOrDefault(Pair.make(m, key), true)));
			m.getCFG().getBlocks()
					.forEach(bb -> bb.setHasRelevantCF(neededCF.getOrDefault(Pair.make(m, bb.idx()), true)));
		}
		success = true;
		return env;
	}

	private void updateNeededDefs(Map<Pair<Method, Integer>, Boolean> neededDefs,
			Map<Pair<Method, Integer>, Boolean> update) {
		for (Pair<Method, Integer> p : update.keySet()) {
			neededDefs.put(p, neededDefs.getOrDefault(p, false) || update.get(p));
		}
	}

	private Value.BitLatticeValue[] createBitMask(int valueWidth, Lattices.Value latticeValue) {
		if (valueWidth != latticeValue.bits.size()) {
			// assert (latticeValue.toString().equals("xx"));
			return Collections.nCopies(valueWidth, Value.BitLatticeValue.UNKNOWN).toArray(new Value.BitLatticeValue[0]);
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