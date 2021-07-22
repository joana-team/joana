package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ApproxMC;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.TempValue;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type;
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

/**
 * Class to encapsulate the combined (nildumu + sat-based) analysis for the channel capacity
 */
public class CombinedAnalysis {

	Environment env;

	public CombinedAnalysis(Environment environment) {
		this.env = environment;
	}

	/**
	 * Returns the channel capacity of the value leaked in {@code leak}
	 * @param leak instruction that leaks the value in question
	 * @param m Method containing the leaking instruction
	 * @return channel capacity
	 */
	public double channelCap(SSAInvokeInstruction leak, Method m) {
		BasicBlock leakBlock = BasicBlock.getBBlockForInstruction(leak, env.iProgram.getEntryMethod().getCFG());

		AnalysisUnit coll = new AnalysisUnit(m);

		double cc = combined(env.segments, leak.getUse(0), leakBlock, m, coll);
		DotGrapher.exportGraph(AnalysisUnit.asGraph());
		return cc;
	}

	/**
	 * Performs the combined analysis
	 *
	 * @param top Parent of all segments to be analysed
	 * @param leaked value number of the leaked value
	 * @param leakBlock basic block where the value is leaked
	 * @param m Method containing the leak
	 * @param coll
	 * @return
	 */
	private double combined(Segment<? extends ProgramPart> top, int leaked, BasicBlock leakBlock, Method m, AnalysisUnit coll) {
		double channelCap = Integer.MAX_VALUE;

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
				channelCap = Math.min(combined(child, leaked, leakBlock, child.programPart.getMethod(), new AnalysisUnit(m)), channelCap);
				coll = new AnalysisUnit(m);

			} else {
				coll.addSegment(child);
			}
		}
		return channelCap;
	}

	/*
		special case: conditional segment, since its children are not analysed sequentially, but in parallel
	 */
	private double combined(ConditionalSegment top, int leaked, BasicBlock leakBlock, Method m, AnalysisUnit coll) {
		double channelCap = Integer.MAX_VALUE;

		double ifCC = Double.MAX_VALUE;
		double elseCC = Double.MAX_VALUE;

		// if-branch
		ContainerSegment ifBranch = top.getIfBranch();
		if (ifBranch.dynAnaFeasible) {
			AnalysisUnit ifUnit = new AnalysisUnit(m);
			ifUnit.addSegment(ifBranch);
			ifUnit.additionalCond = top.branchCondition;
			ifCC = dyn(ifUnit, leaked, leakBlock);
		} else {
			ifCC = combined(ifBranch, leaked, leakBlock, m, new AnalysisUnit(m));
		}

		// else-branch
		ContainerSegment elseBranch = top.getIfBranch();
		if (ifBranch.dynAnaFeasible) {
			AnalysisUnit elseUnit = new AnalysisUnit(m);
			elseUnit.addSegment(ifBranch);
			elseUnit.additionalCond = top.branchCondition;
			elseCC = dyn(elseUnit, leaked, leakBlock);
		} else {
			elseCC = combined(elseBranch, leaked, leakBlock, m, new AnalysisUnit(m));
		}

		return ifCC + elseCC;
	}

	private double dyn(AnalysisUnit as, int leaked, BasicBlock leakBlock) {
		as.finish(true);

		// only dummy blocks? this unit has no influence on channel capacity overall!
		if (as.blocks.stream().allMatch(BasicBlock::isDummy)) {
			as.cc = Type.INTEGER.bitwidth();
			return as.cc;
		}

		List<Integer> valsForMC = (as.blocks.contains(leakBlock)) ?
				Collections.singletonList(leaked) :
				as.collectiveOutputValues;
		if (valsForMC.size() == 0) {
			as.cc = Type.INTEGER.bitwidth();
			return as.cc;
		}

		Pair<Formula, List<Variable>> cc = ccFormula(valsForMC, leakBlock.getCFG().getMethod(), as);

		if (as.additionalCond != null) {
			cc = Pair.make(LogicUtil.ff.and(as.additionalCond, cc.fst), cc.snd);
		}

		ApproxMC mc = new ApproxMC(env.args.outputDirectory);
		long models = Integer.MAX_VALUE;
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

	private Pair<Formula, List<Variable>> ccFormula(List<Integer> leaked, Method m, AnalysisUnit as) {
		Formula f = as.additionalCond;
		List<Variable> priority = new ArrayList<>();

		for (Integer i : leaked) {
			Formula[] deps = m.getDepsForValue(i);
			Variable[] newVars = LogicUtil.createVars(i, deps.length, "leak");
			priority.addAll(Arrays.asList(newVars));
			f = IntStream.range(0, deps.length).mapToObj(j -> LogicUtil.ff.equivalence(deps[j], newVars[j]))
					.reduce(f, LogicUtil.ff::and);
		}
		f = m.getProg().getTempValues().stream().filter(tv -> tv.owningSeg.dynAnaFeasible).map(TempValue::asOpenFormula)
				.reduce(f, LogicUtil.ff::and);
		f = m.getProg().ccRestrictions.stream().reduce(f, LogicUtil.ff::and);
		return Pair.make(f, priority);
	}
}