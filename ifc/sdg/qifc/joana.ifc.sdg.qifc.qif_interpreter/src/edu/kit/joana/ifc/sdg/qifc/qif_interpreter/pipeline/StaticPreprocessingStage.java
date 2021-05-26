package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ConversionException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu.Converter;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu.NildumuOptions;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu.NildumuProgram;
import nildumu.Lattices;
import nildumu.Parser;

import java.util.HashMap;
import java.util.Map;

public class StaticPreprocessingStage implements IStage {

	NildumuOptions options = NildumuOptions.DEFAULT;
	private boolean success = false;

	@Override public Environment execute(Environment env) {

		assert (env.completedStage(Stage.BUILD));

		Converter c = new Converter();
		Parser.ProgramNode p = null;
		try {
			p = c.convertProgram(env.iProgram);
		} catch (ConversionException e) {
			e.printStackTrace();
		}

		env.nProgram = new NildumuProgram(p, options);

		Map<Integer, Lattices.Value> bits = new HashMap<>();
		for (int i : env.iProgram.getEntryMethod().getProgramValues().keySet()) {
			Lattices.Value v = env.nProgram.context.getVariableValue(Converter.varName(i));
			System.out.println(i + " " + v.toString());
			bits.put(i, v);
		}

		env.lastStage = new PreprocessingResult(bits, new HashMap<>());

		return env;
	}

	@Override public boolean success() {
		return success;
	}

	@Override public Stage identity() {
		return Stage.STATIC_PREPROCESSING;
	}

	public static class PreprocessingResult implements IResult {

		public Map<Integer, Lattices.Value> bits;
		public Map<Integer, Boolean> outputInfluece;

		public PreprocessingResult(Map<Integer, Lattices.Value> bits, Map<Integer, Boolean> outputInfluece) {
			this.bits = bits;
			this.outputInfluece = outputInfluece;
		}

		@Override public Stage fromStage() {
			return Stage.STATIC_PREPROCESSING;
		}
	}
}