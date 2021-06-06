package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.IRBuilder;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo.ProgramSegment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGrapher;

import java.io.IOException;

public class BuildStage implements IStage {

	private boolean success = false;
	public static IRBuilder builder;

	@Override public Environment execute(Environment env) {
		assert (env.completedStage(Stage.INIT));
		assert (env.lastStage.fromStage().equals(Stage.INIT));

		InitStage.InitResult data = (InitStage.InitResult) env.lastStage;

		// create SDG
		builder = new IRBuilder(data.classFilePath, data.className, env);
		builder.createBaseSDGConfig();
		try {
			builder.buildAndKeepBuilder();
		} catch (IOException | CancelException | ClassHierarchyException | GraphIntegrity.UnsoundGraphException e) {
			e.printStackTrace();
		}

		env.iProgram = builder.getProgram();
		env.segments = ProgramSegment.create(env.iProgram);

		if (env.args.dumpGraphs) {
			builder.dumpGraph(env.args.outputDirectory);
			for (Method m : env.iProgram.getMethods()) {
				DotGrapher.exportGraph(m.getCFG());
			}
			DotGrapher.exportGraph(env.segments);
		}
		success = true;
		return env;
	}

	@Override public boolean success() {
		return success;
	}

	@Override public Stage identity() {
		return Stage.BUILD;
	}
}