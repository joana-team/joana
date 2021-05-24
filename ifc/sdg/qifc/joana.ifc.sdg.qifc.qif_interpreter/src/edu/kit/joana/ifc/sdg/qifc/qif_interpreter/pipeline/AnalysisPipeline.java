package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.App;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.SimpleLogger;

import java.util.logging.Level;

public class AnalysisPipeline {

	Environment env;

	public void runPipeline(App.Args args) {
		this.env = new Environment(args);
		execute(IStage.Stage.INIT.get());
		execute(IStage.Stage.BUILD.get());

		if (!env.args.onlyRun) {
			// execute(IStage.Stage.STATIC_PREPROCESSING.get());
			execute(IStage.Stage.SAT_ANALYSIS.get());
		}
		execute(IStage.Stage.EXECUTION.get());
	}

	private void execute(IStage stage) {
		SimpleLogger.log(Level.INFO, "Starting: " + stage.identity().toString());
		this.env = stage.execute(this.env);
		env.completedSuccessfully.put(stage.identity(), true);

		if (stage.success()) {
			SimpleLogger.log(Level.INFO, "Finished stage successfully");
		} else {
			SimpleLogger.log(Level.INFO, "Stage failed");
			if (stage.identity().failsFatally) {
				System.exit(1);
			}
		}
	}
}