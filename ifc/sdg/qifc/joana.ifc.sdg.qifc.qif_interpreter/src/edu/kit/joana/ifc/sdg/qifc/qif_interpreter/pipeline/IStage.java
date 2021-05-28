package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public interface IStage {

	Environment execute(Environment env);

	boolean success();

	Stage identity();

	enum Stage {

		INIT(true, "INIT"), BUILD(true, "BUILD"), STATIC_PREPROCESSING(false, "STATIC_PREPROCESSING"), SAT_ANALYSIS(
				true, "SAT_ANALYSIS"), EXECUTION(true, "EXECUTION"), PANIC(true, "PANIC");

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
			objs.put(STATIC_PREPROCESSING, new StaticPreprocessingStage());
		}

		IStage get() {
			return objs.getOrDefault(this, new PanicStage());
		}

		static Deque<Stage> all() {
			Deque<Stage> stages = new ArrayDeque<>();
			stages.add(INIT);
			stages.add(BUILD);
			stages.add(STATIC_PREPROCESSING);
			stages.add(SAT_ANALYSIS);
			stages.add(EXECUTION);
			return stages;
		}
	}

	class PanicStage implements IStage {

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

	interface IResult {
		Stage fromStage();
	}
}