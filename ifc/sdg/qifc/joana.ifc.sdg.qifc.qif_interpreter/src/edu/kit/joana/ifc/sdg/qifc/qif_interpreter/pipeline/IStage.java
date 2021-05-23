package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

public interface IStage {

	Environment execute(Environment env);

	boolean success();

	AnalysisPipeline.Stage identity();

	interface IResult {
		AnalysisPipeline.Stage fromStage();
	}

	class InitResult implements IResult {
		String className;
		String classFilePath;

		@Override public AnalysisPipeline.Stage fromStage() {
			return AnalysisPipeline.Stage.INIT;
		}

		public InitResult(String className, String classFilePath) {
			this.classFilePath = classFilePath;
			this.className = className;
		}
	}

}