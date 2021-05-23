package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.StaticAnalysis;

public class SatAnalysisStage implements IStage {

	private boolean success = false;

	@Override public Environment execute(Environment env) {
		StaticAnalysis sa = new StaticAnalysis(env.iProgram);
		sa.computeSATDeps();
		success = true;
		return env;
	}

	@Override public boolean success() {
		return success;
	}

	@Override public AnalysisPipeline.Stage identity() {
		return AnalysisPipeline.Stage.SAT_ANALYSIS;
	}
}