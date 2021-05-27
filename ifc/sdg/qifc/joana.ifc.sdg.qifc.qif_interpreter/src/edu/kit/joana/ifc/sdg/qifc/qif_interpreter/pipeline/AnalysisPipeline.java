package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.App;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Logger;

public class AnalysisPipeline {

	Environment env;

	public void runPipeline(App.Args args) {
		this.env = new Environment(args);
		execute(IStage.Stage.INIT.get());
		execute(IStage.Stage.BUILD.get());

		if (!env.args.onlyRun) {
			execute(IStage.Stage.STATIC_PREPROCESSING.get());
			execute(IStage.Stage.SAT_ANALYSIS.get());
		}
		execute(IStage.Stage.EXECUTION.get());
	}

	private void execute(IStage stage) {
		Logger.logEval("Starting: " + stage.identity().toString());
		this.env = stage.execute(this.env);
		env.completedSuccessfully.put(stage.identity(), true);

		if (stage.success()) {
			Logger.logEval("Finished stage successfully");
		} else {
			Logger.logEval("Stage failed");
			if (stage.identity().failsFatally) {
				System.exit(1);
			}
		}
	}
}