package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.App;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.SimpleLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class AnalysisPipeline {

	Environment env;

	public void runPipeline(App.Args args) {
		this.env = new Environment(args);
		execute(Stage.INIT.get());
		execute(Stage.BUILD.get());

		if (!env.args.onlyRun) {
			execute(Stage.SAT_ANALYSIS.get());
		}
		execute(Stage.EXECUTION.get());
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

	public enum Stage {

		INIT(true, "INIT"), BUILD(true, "BUILD"), NILDUMU_BUILD(false, "NILDUMU_BUILD"), STATIC_PREPROCESSING(false,
				"STATIC_PREPROCESSING"), SAT_ANALYSIS(true, "SAT_ANALYSIS"), EXECUTION(true, "EXECUTION"), PANIC(true,
				"PANIC");

		boolean failsFatally;
		String name;

		Stage(boolean failsFatally, String name) {
			this.failsFatally = failsFatally;
			this.name = name;
		}

		@Override public String toString() {
			return name;
		}

		static Map<Stage, IStage> objs = new HashMap<>();

		static {
			objs.put(INIT, new InitStage());
			objs.put(BUILD, new BuildStage());
			objs.put(SAT_ANALYSIS, new SatAnalysisStage());
			objs.put(EXECUTION, new ExecutionStage());
		}

		IStage get() {
			return objs.getOrDefault(this, new PanicStage());
		}
	}

	public static class PanicStage implements IStage {

		@Override public Environment execute(Environment env) {
			return env;
		}

		@Override public boolean success() {
			return false;
		}

		@Override public Stage identity() {
			return Stage.PANIC;
		}
	}

}