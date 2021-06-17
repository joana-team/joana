package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ApproxMC;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.Environment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGrapher;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class CombinedAnalysis {

	Environment env;

	public CombinedAnalysis(Environment environment) {
		this.env = environment;
	}

	public double channelCap(SSAInvokeInstruction leak, Method m) {
		BasicBlock leakBlock = BasicBlock.getBBlockForInstruction(leak, env.iProgram.getEntryMethod().getCFG());
		double cc = combined(env.segments, leak.getUse(0), leakBlock, m);
		DotGrapher.exportGraph(AnalysisUnit.asGraph());
		return cc;
	}

	private double combined(Segment<? extends ProgramPart> top, int leaked, BasicBlock leakBlock, Method m) {
		double channelCap = Integer.MAX_VALUE;
		AnalysisUnit coll = new AnalysisUnit(m);

		if (!top.dynAnaFeasible) {
			double statCC = stat((IStaticAnalysisSegment) top);
			coll.finish(false);
			coll.cc = statCC;
			return statCC;
		}

		for (Segment<? extends ProgramPart> child : top.children) {

			if (child.owns(leakBlock)) {
				coll.addSegment(child);
				double segmentCC = dyn(coll, leaked, leakBlock);
				return Math.min(segmentCC, channelCap);
			}

			if (child.hasStatAnaChild()) {
				double segmentCC = dyn(coll, leaked, leakBlock);
				channelCap = Math.min(segmentCC, channelCap);
				channelCap = Math.min(combined(child, leaked, leakBlock, child.programPart.getMethod()), channelCap);
				coll = new AnalysisUnit(m);
			} else {
				coll.addSegment(child);
			}
		}
		return channelCap;
	}

	private double dyn(AnalysisUnit as, int leaked, BasicBlock leakBlock) {
		as.finish(true);

		List<Integer> valsForMC = (as.blocks.contains(leakBlock)) ?
				Collections.singletonList(leaked) :
				as.collectiveOutputValues;

		Pair<Formula, List<Variable>> cc = ccFormula(valsForMC, leakBlock.getCFG().getMethod());
		ApproxMC mc = new ApproxMC(env.args.outputDirectory);
		int models = Integer.MAX_VALUE;
		try {
			models = mc.estimateModelCount(cc.fst, cc.snd);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		double cap = Math.log(models) / Math.log(2);
		as.cc = cap;
		return cap;
	}

	private double stat(IStaticAnalysisSegment segment) {
		return segment.channelCap(env);
	}

	private Pair<Formula, List<Variable>> ccFormula(List<Integer> leaked, Method m) {
		Formula f = LogicUtil.ff.constant(true);
		List<Variable> priority = new ArrayList<>();

		for (Integer i : leaked) {
			Formula[] deps = m.getDepsForValue(i);
			Variable[] newVars = LogicUtil.createVars(i, deps.length, "leak");
			priority.addAll(Arrays.asList(newVars));
			f = IntStream.range(0, deps.length).mapToObj(j -> LogicUtil.ff.equivalence(deps[j], newVars[j]))
					.reduce(f, LogicUtil.ff::and);
		}

		return Pair.make(f, priority);
	}
}