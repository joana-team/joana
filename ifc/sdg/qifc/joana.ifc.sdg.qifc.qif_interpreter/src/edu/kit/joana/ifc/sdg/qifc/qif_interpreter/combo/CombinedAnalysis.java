package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ApproxMC;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.Environment;
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

	public double channelCap(SSAInvokeInstruction leak) {
		BasicBlock leakBlock = BasicBlock.getBBlockForInstruction(leak, env.iProgram.getEntryMethod().getCFG());
		return combined(env.segments, leak.getUse(0), leakBlock);
	}

	private double combined(Segment<? extends ProgramPart> top, int leaked, BasicBlock leakBlock) {
		double channelCap = Integer.MAX_VALUE;

		if (!top.dynAnaFeasible) {
			return stat((IStaticAnalysisSegment) top);
		}

		List<Segment<? extends ProgramPart>> collectiveDyn = new ArrayList<>();

		for (Segment<? extends ProgramPart> child : top.children) {
			if (child.owns(leakBlock)) {
				collectiveDyn.add(child);
				double segmentCC = dyn(collectiveDyn, leaked, leakBlock);
				channelCap = Math.min(segmentCC, channelCap);
			}

			if (child.hasStatAnaChild()) {
				double segmentCC = dyn(collectiveDyn, leaked, leakBlock);
				channelCap = Math.min(segmentCC, channelCap);
				channelCap = Math.min(combined(child, leaked, leakBlock), channelCap);
				collectiveDyn = new ArrayList<>();
			} else {
				collectiveDyn.add(child);
			}
		}
		return channelCap;
	}

	private double dyn(List<Segment<? extends ProgramPart>> segments, int leaked, BasicBlock leakBlock) {
		segments.forEach(Segment::finalize);
		List<Integer> outs = (segments.stream().anyMatch(s -> s.owns(leakBlock))) ?
				Collections.singletonList(leaked) :
				collectiveOuts(segments);

		Pair<Formula, List<Variable>> cc = ccFormula(outs, leakBlock.getCFG().getMethod());
		ApproxMC mc = new ApproxMC(env.args.outputDirectory);
		int models = Integer.MAX_VALUE;
		try {
			models = mc.estimateModelCount(cc.fst, cc.snd);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		return Math.log(models) / Math.log(2);
	}

	private List<Integer> collectiveOuts(List<Segment<? extends ProgramPart>> segments) {
		List<Integer> outs = new ArrayList<>();
		for (Segment<? extends ProgramPart> s : segments) {
			outs.addAll(s.outputs);
		}
		return outs;
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