package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.exec.Interpreter;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ParameterException;

public class ExecutionStage implements IStage {

	private boolean success;

	@Override public Environment execute(Environment env) {
		Interpreter i = new Interpreter(env.iProgram);
		try {
			i.execute(env.args.args);
			success = true;
		} catch (ParameterException | OutOfScopeException e) {
			e.printStackTrace();
			success = false;
		}
		return env;
	}

	@Override public boolean success() {
		return success;
	}

	@Override public AnalysisPipeline.Stage identity() {
		return AnalysisPipeline.Stage.EXECUTION;
	}
}