package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.App;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Logger;

import java.util.Deque;

public class AnalysisPipeline {

	public Environment env;

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
		if (!args.pp) {
			stages.remove(IStage.Stage.STATIC_PREPROCESSING);
		}

		if (args.onlyStatic) {
			stages.remove(IStage.Stage.EXECUTION);
		}
		return stages;
	}

	private void execute(IStage stage) {
		Logger.startPipelineStage(stage.identity());
		this.env = stage.execute(this.env);
		env.completedSuccessfully.put(stage.identity(), true);
		Logger.finishPipelineStage(stage.identity(), stage.success());

		if (!stage.success() && stage.identity().failsFatally) {
			System.out.println("Failed stage: " + stage.identity());
			//System.exit(1);
		}
	}
}