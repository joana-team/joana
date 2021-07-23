package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn;

import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.Config;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.Environment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import org.logicng.formulas.Formula;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LoopHandler {

	public static LoopBody buildSkeleton(Method m, BasicBlock head) {
		assert (head.isLoopHeader());
		LoopBody loop = new LoopBody(m, head);
		return loop;
	}

	/**
	 * analyzes loopbody and computes data and cf dependencies
	 * sets dependency fields for computed values accordingly
	 *
	 * @param m    Method object that contains the Loop
	 * @param head Loop header -- has already been visited by {@code sv}, to initialize in-loop variables
	 * @param sa   StaticAnalysis object that is responsible for analysing the whole program
	 * @return {@code LoopBody} object that contains all necessary loop information
	 */
	public static LoopBody analyze(Method m, BasicBlock head, SATAnalysis sa, LoopBody base, Environment env) {
		int loopUnrollingMax = Config.unwind;

		base.computeLoopCondition();
		extractDeps(m, base);
		List<Integer> visited = new ArrayList<>();
		Queue<BasicBlock> toVisit = new ArrayDeque<>();

		edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.LoopSATVisitor sv = new LoopSATVisitor(sa, base, env);

		// here we safe all blocks that are successors of blocks that end in a conditional jump out of the loop
		List<BasicBlock> breakSuccessors = new ArrayList<>();

		// no need to visit head again --> add in-loop successor
		toVisit.add(head.succs().stream().filter(base.getBlocks()::contains).findFirst().get());
		visited.add(head.idx());

		// visit all loop blocks -- treat like separate function, where input variables are the variables assigned to the phi-def-values
		// in the loop header
		while (!toVisit.isEmpty()) {
			BasicBlock b = toVisit.poll();
			visited.add(b.idx());

			try {
				sv.visitBlock(m, b, -1);
			} catch (OutOfScopeException e) {
				e.printStackTrace();
			}

			if (b.isLoopHeader() && !b.equals(head)) {
				LoopBody l = LoopHandler
						.analyze(m, b, sa, m.getLoops().stream().filter(loop -> loop.getHead().equals(b)).findFirst().get(), env);
				m.addLoop(l);
				toVisit.addAll(
						b.succs().stream().filter(succ -> !l.getBlocks().contains(succ)).collect(Collectors.toList()));
			} else {
				breakSuccessors.addAll(b.succs().stream().filter(succ -> !base.hasBlock(succ.idx()))
						.collect(Collectors.toList()));
				for (BasicBlock succ : b.succs()) {
					if (base.getBlocks().contains(succ) && !succ.equals(head) && (succ.isLoopHeader() || succ.preds()
							.stream().mapToInt(BasicBlock::idx).allMatch(visited::contains))) {
						toVisit.add(succ);
					}
				}
			}
		}

		base.setOutDT(sv.getCurrentLeaf().root());
		// if the loop has a (or more) break statements,
		// we visit the basic blocks that connect the jump out of the loop with the after-loop successor-block of the loop header
		BasicBlock afterLoop = head.succs().stream().filter(succ -> !base.hasBlock(succ.idx())).findFirst().get()
				.succs().get(0);
		List<Integer> visitedBreakBlocks = new ArrayList<>();
		while (!breakSuccessors.isEmpty()) {
			BasicBlock b = breakSuccessors.remove(0);
			visitedBreakBlocks.add(b.idx());
			try {
				sv.visitBlock(m, b, -1);
			} catch (OutOfScopeException e) {
				e.printStackTrace();
			}
			b.succs().stream().filter(succ -> !visitedBreakBlocks.contains(succ.idx()))
					.filter(succ -> !succ.equals(afterLoop)).filter(succ -> !breakSuccessors.contains(succ))
					.forEach(breakSuccessors::add);
		}

		extractOutDeps(base);
		computeRuns(base, m, loopUnrollingMax + 1);

		// combine all possible loop results into a single formula and set the value dependencies in the Value objects accordingly
		Map<Integer, TempValue> tempValues = new HashMap<>();
		base.lastRun().getPrimitive().keySet().forEach(i -> tempValues
				.put(i, new TempValue(i, m, base, m.getValue(i).getConstantBitMask(), base.lastRun().getPrimitive(i))));
		for (int i = loopUnrollingMax; i >= 0; i--) {
			LoopIteration run = base.getRun(i);
			for (Integer j : base.getIn().keySet()) {
				tempValues.get(j).setReal(LogicUtil
						.ternaryOp(LogicUtil.ff.and(run.getReached(), run.getJumpOutAfterThisIteration()),
								run.getPrimitive(j), tempValues.get(j).real));
				tempValues.get(j).setRestricted(LogicUtil
						.ternaryOp(run.getJumpOutAfterThisIteration(), run.getPrimitive(j), tempValues.get(j).real));
			}
		}

		Formula valueRestriction = IntStream.range(0, base.getSimulatedIterationNum())
				.mapToObj(i -> base.getRun(i).getJumpOutAfterThisIteration())
				.reduce(LogicUtil.ff.constant(false), LogicUtil.ff::or);
		//tempValues.values().forEach(t -> t.valueRestriction = valueRestriction);
		m.getProg().dlRestrictions.add(valueRestriction);
		m.getProg().addTemporaryValue(tempValues.values());

		// same thing, but for arrays
		for (int i : base.getPlaceholderArrays().keySet()) {
			Formula[][] res = base.lastRun().getArray(i).clone();
			for (int j = base.getSimulatedIterationNum(); j >= 0; j--) {
				int finalJ = j;
				IntStream.range(0, res.length).forEach(k -> res[k] = LogicUtil
						.ternaryOp(base.getRun(finalJ).getJumpOutAfterThisIteration(),
								base.getRun(finalJ).getArray(i)[k], res[k]));
			}
			m.getArray(i).setValueDependencies(res);
		}

		return base;
	}

	/**
	 * returns a Map of pairs, that describes the program's values after a certain number of loop iterations. The Map entry w/ key i corresponds to the values after the i-th iteration
	 * The 0th entry means, the loop was not executed at all.
	 * <p>
	 * The first element of a pair describes the programs values, the snd element describes the condition that must hold for the loop execution to be stopped after this iteration
	 */
	private static void computeRuns(LoopBody loop, Method m, int loopUnrollingMax) {
		LoopIteration beforeLoop = new LoopIteration(0);

		for (int i : m.getProgramValues().keySet()) {
			if (m.getValue(i).isArrayType()) {
				beforeLoop.addArr(i, ((Array<? extends Value>) m.getValue(i)).getValueDependencies());
			} else {
				Formula[] before = new Formula[m.getDepsForValue(i).length];
				IntStream.range(0, before.length).forEach(k -> before[k] = (loop.getIn().containsKey(i)) ?
						loop.getBeforeLoop(i)[k] :
						m.getDepsForValue(i)[k]);
				beforeLoop.addPrimitiveVal(i, before);
			}
		}
		beforeLoop.setJumpOutAfterThisIteration(LogicUtil.applySubstitution(loop.getJumpOut(), beforeLoop.makeSubstitution(loop.getIn(), loop.getPlaceholderArrayVars())));
		beforeLoop.setJumpOutOnlyLoopCondition(LogicUtil.applySubstitution(loop.getJumpOutOnlyLoopCondition(), beforeLoop.makeSubstitution(loop.getIn(), loop.getPlaceholderArrayVars())));
		loop.addIteration(beforeLoop);

		for (int i = 1; i < loopUnrollingMax; i++) {
			LoopIteration iteration = loop.simulateRun(loop.getRun(i - 1));
			loop.addIteration(iteration);
		}
	}

	/*
	Method to populate the in-value Map, out-value Map and beforeLoop Map of the LoopBody l
	 */
	private static void extractDeps(Method m, LoopBody loop) {
		loop.phiMapping().stream().filter(t -> m.getValue(t.getLeft()).influencesLeak()).forEach(t -> {
			loop.addInDeps(t.getLeft(), m.getVarsForValue(t.getLeft()));
			loop.addResultMapping(t.getLeft(), t.getRight());
			loop.addBeforeLoopDeps(t.getLeft(), m.getDepsForValue(t.getRight()));
		});
		loop.generateInitialValueSubstitution();
	}

	private static void extractOutDeps(LoopBody loop) {
		loop.phiMapping().forEach(t -> loop.addOutDeps(t.getLeft(), t.getRight()));
	}

	/**
	 * Function to compute a formula that describes the result value of a phi-instruction that combines control flow paths from loop head and a possible break in the loop
	 *
	 * @param l          LoopBody object of the loop where we get the valeu from
	 * @param def        value number that is assigned the result of the phi-instruction
	 * @param normalUse  valNum of value that is assigned if the loop is exited "normally" i.e. not through a break
	 * @param breakUse   valNum of value that is assigned if the loop is exited through a break
	 * @param breakBlock block 'between' the loop and the block where def is defined
	 * @return Formula that describes the value assigned to value number {@code def}, depending on whether the loop was exited 'normally' or through a break
	 */
	public static Formula[] computeBreakValues(LoopBody l, int def, int normalUse, int breakUse,
			BasicBlock breakBlock) {
		Pair<Formula, Formula[]> breakRes = multipleBreaksCondition(l, breakUse, breakBlock);

		Formula breakCondition = breakRes.fst;
		Formula exitLoopCondition;

		Formula[] temp = LogicUtil.createVars(def, l.getOwner().getValue(normalUse).getWidth(), "t");
		Formula[] beforeLoop = l.getBeforeLoop(normalUse);
		Formula[] atBreak = breakRes.snd;
		Formula[] res;

		// base result when loop is not taken
		res = LogicUtil.ternaryOp(l.beforeLoop().getJumpOutOnlyLoopCondition(), beforeLoop, temp);

		for (int i = 1; i < l.getSimulatedIterationNum(); i++) {
			Formula breakThisIteration = breakCondition.substitute(l.getRun(i - 1).makeSubstitution(l.getIn(), l.getPlaceholderArrayVars()).toLogicNGSubstitution());
			Formula[] breakResult = LogicUtil.applySubstitution(atBreak, l.getRun(i - 1).makeSubstitution(l.getIn(), l.getPlaceholderArrayVars()));
			exitLoopCondition = l.getRun(i).getJumpOutOnlyLoopCondition();
			Formula[] afterLoop = l.getRun(i).getPrimitive(normalUse);

			Formula[] iterationResult;

			if (i == l.getSimulatedIterationNum() - 1) {
				iterationResult = LogicUtil.ternaryOp(breakThisIteration, breakResult, afterLoop);
			} else {
				iterationResult = LogicUtil.ternaryOp(breakThisIteration, breakResult,
						LogicUtil.ternaryOp(exitLoopCondition, afterLoop, temp));
			}
			Substitution s = new Substitution();
			s.addMapping(temp, iterationResult);
			res = LogicUtil.applySubstitution(res, s);
		}
		return res;
	}

	/*
	First component of return value: Condition to check whether any conditional jump out of the loop occurs
	Second component of return value: Value of {@code breakUse} depending on which conditional jump was taken
	 */
	private static Pair<Formula, Formula[]> multipleBreaksCondition(LoopBody l, int breakUse, BasicBlock breakBlock) {
		// we might have multiple breaks in the loop. Here we collect all values that could possibly be our "output" value, i.e. the value that is ultimately assigned to the break-phi,
		// possibly though multiple phi-instructions in between
		// The second component of each pair describes the implicit information contained in each value
		List<Pair<Integer, IFTreeNode>> possibleValues = findBreakValuesRec(l, breakUse, breakBlock, new ArrayList<>());

		int valnum = possibleValues.get(possibleValues.size() - 1).fst;
		Formula[] value = (l.getIn().containsKey(valnum) ?
				l.getOwner().getVarsForValue(valnum) :
				l.getOwner().getDepsForValue(valnum));

		for (int i = 0; i < possibleValues.size() - 1; i++) {
			valnum = possibleValues.get(i).fst;
			Formula[] breakValue = l.getIn().containsKey(valnum) ?
					l.getOwner().getVarsForValue(valnum) :
					l.getOwner().getDepsForValue(valnum);
			value = LogicUtil.ternaryOp(possibleValues.get(i).snd.getImplicitFlowFormula(), breakValue, value);
		}

		Formula condJumpTaken = possibleValues.stream().map(p -> p.snd).map(IFTreeNode::getImplicitFlowFormula)
				.reduce(LogicUtil.ff.constant(false), LogicUtil.ff::or);

		return Pair.make(condJumpTaken, value);
	}

	private static List<Pair<Integer, IFTreeNode>> findBreakValuesRec(LoopBody l, int breakUse, BasicBlock block,
			List<Pair<Integer, IFTreeNode>> vals) {

		// skip dummy blocks
		if (block.isDummy()) {
			return findBreakValuesRec(l, breakUse, block.preds().get(0), vals);
		}

		// value is result of phi-instruction. This implies there are more break-statements in the loop where could have jumped out
		// we add both uses to our list and see where they are defined
		if (block.ownsValue(breakUse) && block.instructions().stream()
				.anyMatch(i -> i.hasDef() && i.getDef() == breakUse && i instanceof SSAPhiInstruction)) {
			SSAPhiInstruction instruction = (SSAPhiInstruction) block.instructions().stream()
					.filter(i -> i instanceof SSAPhiInstruction && i.getDef() == breakUse).findFirst().get();
			vals = findBreakValuesRec(l, instruction.getUse(0), block.preds().get(0), vals);
			vals = findBreakValuesRec(l, instruction.getUse(1), block.preds().get(1), vals);
			return vals;
		} else {

			// base case: add value to our possible values and return
			// the block might have multiple predecessors --> multiple break locations return the same value
			// we add all possible break-locations separately bc they represent different control flow information
			for (BasicBlock pred : block.preds()) {
				vals.add(Pair.make(breakUse, pred.getIfTree()));
			}
			return vals;
		}
	}
}