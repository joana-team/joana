package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.SATAnalysis;

public class SatAnalysisStage implements IStage {

	private boolean success = false;

	@Override public Environment execute(Environment env) {
		SATAnalysis sa = new SATAnalysis(env);
		sa.computeSATDeps();
		success = true;
		return env;
	}

	@Override public boolean success() {
		return success;
	}

	@Override public Stage identity() {
		return Stage.SAT_ANALYSIS;
	}
}