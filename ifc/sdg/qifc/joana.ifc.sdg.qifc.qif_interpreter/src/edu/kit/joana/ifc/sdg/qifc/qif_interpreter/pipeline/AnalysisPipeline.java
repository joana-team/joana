package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.App;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Logger;

import java.util.Deque;

public class AnalysisPipeline {

	Environment env;

	public void runPipeline(App.Args args) {
		runPipelineUntil(args, IStage.Stage.EXECUTION);
	}

	public void runPipelineUntil(App.Args args, IStage.Stage until) {
		Deque<IStage.Stage> stages = prepStages(args, until);
		this.env = new Environment(args);
		while (!stages.isEmpty()) {
			IStage.Stage next = stages.pollFirst();
			execute(next.get());
		}
	}

	public Deque<IStage.Stage> prepStages(App.Args args, IStage.Stage until) {
		Deque<IStage.Stage> stages = IStage.Stage.all();
		while (stages.getLast() != until) {
			stages.pollLast();
		}
		if (args.onlyRun) {
			stages.remove(IStage.Stage.STATIC_PREPROCESSING);
			stages.remove(IStage.Stage.SAT_ANALYSIS);
		}
		return stages;
	}

	private void execute(IStage stage) {
		Logger.logEval("Starting: " + stage.identity().toString());
		this.env = stage.execute(this.env);
		env.completedSuccessfully.put(stage.identity(), true);

		if (stage.success()) {
			Logger.logEval("Finished: " + stage.identity().toString());
		} else {
			Logger.logEval("Failed: " + stage.identity().toString());
			if (stage.identity().failsFatally) {
				System.exit(1);
			}
		}
	}
}