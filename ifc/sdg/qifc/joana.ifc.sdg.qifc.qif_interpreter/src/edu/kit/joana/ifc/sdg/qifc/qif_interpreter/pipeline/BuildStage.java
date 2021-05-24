package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.IRBuilder;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGrapher;

import java.io.IOException;

public class BuildStage implements IStage {

	private boolean success = false;

	@Override public Environment execute(Environment env) {
		assert (env.completedStage(AnalysisPipeline.Stage.INIT));
		assert (env.lastStage.fromStage().equals(AnalysisPipeline.Stage.INIT));

		InitResult data = (InitResult) env.lastStage;

		// create SDG
		IRBuilder builder = new IRBuilder(data.classFilePath, data.className);
		builder.createBaseSDGConfig();
		try {
			builder.buildAndKeepBuilder();
		} catch (IOException | CancelException | ClassHierarchyException | GraphIntegrity.UnsoundGraphException e) {
			e.printStackTrace();
		}

		env.iProgram = builder.getProgram();

		if (env.args.dumpGraphs) {
			builder.dumpGraph(env.args.outputDirectory);
			for (Method m : env.iProgram.getMethods()) {
				DotGrapher.exportDotGraph(m.getCFG());
			}
		}
		success = true;
		return env;
	}

	private void initValues(Method m) {

	}

	@Override public boolean success() {
		return success;
	}

	@Override public AnalysisPipeline.Stage identity() {
		return AnalysisPipeline.Stage.BUILD;
	}
}