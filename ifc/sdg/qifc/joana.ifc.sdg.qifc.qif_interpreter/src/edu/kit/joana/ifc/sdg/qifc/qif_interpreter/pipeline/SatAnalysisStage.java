package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo.CombinedAnalysis;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.SATAnalysis;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.SATVisitor;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Logger;

import java.util.Arrays;

public class SatAnalysisStage implements IStage {

	private boolean success = false;

	@Override public Environment execute(Environment env) {
		SATAnalysis sa = new SATAnalysis(env);
		sa.computeSATDeps();
		Logger.finishPipelineStage(Stage.SAT_ANALYSIS, true);
		Logger.startPipelineStage(Stage.HYBRID);
		CombinedAnalysis ca = new CombinedAnalysis(env);
		SSAInvokeInstruction leak = (SSAInvokeInstruction) Arrays
				.stream(env.iProgram.getEntryMethod().getIr().getInstructions())
				.filter(i -> i instanceof SSAInvokeInstruction && ((SSAInvokeInstruction) i).getDeclaredTarget()
						.getSignature().equals(SATVisitor.OUTPUT_FUNCTION)).findFirst().get();
		double cc = ca.channelCap(leak, env.iProgram.getEntryMethod());
		System.out.println("Channel capacity: " + cc);
		Logger.finishPipelineStage(Stage.HYBRID, true);
		success = true;
		return env;
	}

	@Override public boolean success() {
		return success;
	}

	@Override public Stage identity() {
		return Stage.SAT_ANALYSIS;
	}
}