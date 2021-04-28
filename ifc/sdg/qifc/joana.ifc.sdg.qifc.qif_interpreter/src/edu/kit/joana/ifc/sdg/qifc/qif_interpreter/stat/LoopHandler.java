package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import org.logicng.formulas.Formula;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LoopHandler {

	/**
	 * analyzes loopbody and computes data and cf dependencies
	 * sets dependency fields for computed values accordingly
	 *
	 * @param m    Method object that contains the Loop
	 * @param head Loop header -- has already been visited by {@code sv}, to initialize in-loop variables
	 * @param sa   StaticAnalysis object that is responsible for analysing the whole program
	 * @return {@code LoopBody} object that contains all necessary loop information
	 */
	public static LoopBody analyze(Method m, BBlock head, StaticAnalysis sa) {
		int loopUnrollingMax = m.getProg().getConfig().loopUnrollingMax();

		assert (head.isLoopHeader());
		LoopBody loop = new LoopBody(m, head);

		List<Integer> visited = new ArrayList<>();
		Queue<BBlock> toVisit = new ArrayDeque<>();

		SATVisitor sv = new LoopSATVisitor(sa, loop);

		// here we safe all blocks that are successors of blocks that end in a conditional jump out of the loop
		List<BBlock> breakSuccessors = new ArrayList<>();

		// no need to visit head again --> add in-loop successor
		toVisit.add(head.succs().stream().filter(loop.getBlocks()::contains).findFirst().get());
		visited.add(head.idx());

		// visit all loop blocks -- treat like separate function, where input variables are the variables assigned to the phi-def-values
		// in the loop header
		while (!toVisit.isEmpty()) {
			BBlock b = toVisit.poll();
			visited.add(b.idx());

			try {
				sv.visitBlock(m, b, -1);
			} catch (OutOfScopeException e) {
				e.printStackTrace();
			}

			if (b.isLoopHeader() && !b.equals(head)) {
				LoopBody l = LoopHandler.analyze(m, b, sa);
				m.addLoop(l);
				toVisit.addAll(
						b.succs().stream().filter(succ -> !l.getBlocks().contains(succ)).collect(Collectors.toList()));
			} else {
				breakSuccessors.addAll(b.succs().stream().filter(succ -> !loop.hasBlock(succ.idx()))
						.collect(Collectors.toList()));
				for (BBlock succ : b.succs()) {
					if (loop.getBlocks().contains(succ) && !succ.equals(head) && (succ.isLoopHeader() || succ.preds()
							.stream().mapToInt(BBlock::idx).allMatch(visited::contains))) {
						toVisit.add(succ);
					}
				}
			}
		}

		// if the loop has a (or more) break statements,
		// we visit the basic blocks that connect the jump out of the loop with the after-loop successor-block of the loop header
		BBlock afterLoop = head.succs().stream().filter(succ -> !loop.hasBlock(succ.idx())).findFirst().get().succs()
				.get(0);
		List<Integer> visitedBreakBlocks = new ArrayList<>();
		while (!breakSuccessors.isEmpty()) {
			BBlock b = breakSuccessors.remove(0);
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

		extractDeps(m, head, loop);
		computeRuns(loop, m, loopUnrollingMax);

		// combine all possible loop results into a single formula and set the value dependencies in the Value objects accordingly
		loop.lastRun().getPrimitive().keySet().forEach(i -> m.setDepsForvalue(i, loop.lastRun().getPrimitive(i)));
		for (int i = loopUnrollingMax - 2; i >= 0; i--) {
			LoopIteration run = loop.getRun(i);
			loop.getIn().keySet().forEach(
					j -> m.setDepsForvalue(j, LogicUtil.ternaryOp(run.getJumpOutAfterThisIteration(), run.getPrimitive(j), m.getDepsForValue(j))));
		}

		// TODO do the same for arrays

		return loop;
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

		loop.addIteration(beforeLoop);

		for (int i = 1; i < loopUnrollingMax; i++) {
			LoopIteration iteration = loop.simulateRun(loop.getRun(i - 1));
			loop.addIteration(iteration);
		}
	}

	/*
	Method to populate the in-value Map, out-value Map and beforeLoop Map of the LoopBody l
	 */
	private static void extractDeps(Method m, BBlock head, LoopBody loop) {
		Iterator<ISSABasicBlock> orderedPredsIter = head.getCFG().getWalaCFG().getPredNodes(head.getWalaBasicBlock());

		// find position i of the phi-use value that belongs to the head's in-loop predecessor
		int argNum = 0;
		while (orderedPredsIter.hasNext()) {
			int blockNum = orderedPredsIter.next().getNumber();
			if (loop.getBlocks().stream().anyMatch(b -> b.idx() == blockNum)) {
				break;
			}
			argNum++;
		}

		// copy deps to loop
		for (SSAInstruction i : head.instructions()) {
			if (i instanceof SSAPhiInstruction) {
				loop.addInDeps(i.getDef(), m.getVarsForValue(i.getDef()));
				loop.addOutDeps(i.getDef(), i.getUse(argNum));
				loop.addBeforeLoopDeps(i.getDef(), m.getDepsForValue(i.getUse(1 - argNum)));
			}
		}
		loop.generateInitialValueSubstitution();
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
	public static Formula[] computeBreakValues(LoopBody l, int def, int normalUse, int breakUse, BBlock breakBlock) {
		Pair<Formula, Formula[]> breakRes = multipleBreaksCondition(l, breakUse, breakBlock);

		Formula breakCondition = breakRes.fst;
		Formula exitLoopCondition;

		Formula[] temp = LogicUtil.createVars(def, l.getOwner().getValue(normalUse).getWidth(), "t");
		Formula[] beforeLoop = l.getBeforeLoop(normalUse);
		Formula[] atBreak = breakRes.snd;
		Formula[] res;

		// base result when loop is not taken
		res = LogicUtil.ternaryOp(l.beforeLoop().getJumpOutAfterThisIteration(), beforeLoop, temp);

		for (int i = 1; i < l.getSimulatedIterationNum(); i++) {
			Formula breakThisIteration = breakCondition.substitute(l.getRun(i - 1).makeSubstitution(l.getIn(), l.getPlaceholderArrayVars()).toLogicNGSubstitution());
			Formula[] breakResult = LogicUtil.applySubstitution(atBreak, l.getRun(i - 1).makeSubstitution(l.getIn(), l.getPlaceholderArrayVars()));
			exitLoopCondition = l.getRun(i).getJumpOutAfterThisIteration();
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
	private static Pair<Formula, Formula[]> multipleBreaksCondition(LoopBody l, int breakUse, BBlock breakBlock) {
		// we might have multiple breaks in the loop. Here we collect all values that could possibly be our "output" value, i.e. the value that is ultimately assigned to the break-phi,
		// possibly though multiple phi-instructions in between
		// The second component of each pair describes the implicit information contained in each value
		List<Pair<Integer, List<Pair<Integer, Boolean>>>> possibleValues = findBreakValuesRec(l, breakUse, breakBlock,
				new ArrayList<>());
		possibleValues.sort((o1, o2) -> new IFComparator().compare(o1.snd, o2.snd));

		int valnum = possibleValues.get(possibleValues.size() - 1).fst;
		Formula[] value = (l.getIn().containsKey(valnum) ? l.getOwner().getVarsForValue(valnum) : l.getOwner().getDepsForValue(valnum));

		for (int i = 0; i < possibleValues.size() - 1; i++) {
			valnum = possibleValues.get(i).fst;
			Formula[] breakValue = l.getIn().containsKey(valnum) ? l.getOwner().getVarsForValue(valnum) : l.getOwner().getDepsForValue(valnum);
			value = LogicUtil.ternaryOp(BBlock.generateImplicitFlowFormula(possibleValues.get(i).snd, l.getOwner().getCFG()), breakValue, value);
		}

		Formula condJumpTaken = possibleValues.stream().map(p -> p.snd).map(list -> BBlock.generateImplicitFlowFormula(list, l.getHead()
				.getCFG())).reduce(LogicUtil.ff.constant(false), LogicUtil.ff::or);

		return Pair.make(condJumpTaken, value);
	}

	private static List<Pair<Integer, List<Pair<Integer, Boolean>>>> findBreakValuesRec(LoopBody l, int breakUse, BBlock block, List<Pair<Integer, List<Pair<Integer, Boolean>>>> vals) {

		// skip dummy blocks
		if (block.isDummy()) {
			return findBreakValuesRec(l, breakUse, block.preds().get(0), vals);
		}

		// value is result of phi-instruction. This implies there are more break-statements in the loop where could have jumped out
		// we add both uses to our list and see where they are defined
		if (block.ownsValue(breakUse) && block.instructions().stream().anyMatch(i -> i.hasDef() && i.getDef() == breakUse && i instanceof SSAPhiInstruction)) {
			SSAPhiInstruction instruction = (SSAPhiInstruction) block.instructions().stream().filter(i -> i instanceof SSAPhiInstruction && i.getDef() == breakUse).findFirst().get();
			vals = findBreakValuesRec(l, instruction.getUse(0), block.preds().get(0), vals);
			vals = findBreakValuesRec(l, instruction.getUse(1), block.preds().get(1), vals);
			return vals;
		} else {

			// base case: add value to our possible values and return
			// the block might have multiple predecessors --> multiple break locations return the same value
			// we add all possible break-locations separately bc they represent different control flow information
			for (BBlock pred: block.preds()) {
				List<Pair<Integer, Boolean>> loopImplicitFlow = pred.getImplicitFlows().stream()
						.filter(p -> l.hasBlock(p.fst) && !(l.getHead().idx() == p.fst))
						.sorted(Comparator.comparingInt(o -> o.fst)).collect(Collectors.toList());
				vals.add(Pair.make(breakUse, loopImplicitFlow));
			}

			return vals;
		}
	}

	private static class IFComparator implements Comparator<List<Pair<Integer, Boolean>>> {

		@Override public int compare(List<Pair<Integer, Boolean>> o1, List<Pair<Integer, Boolean>> o2) {
			int i = 0;
			while (i < o1.size() && i < o2.size() && o1.get(i).fst.equals(o2.get(i).fst)) {
				i++;
			}
			if (o1.size() <= i) {
				return 1;
			} else if (o2.size() <= i) {
				return -1;
			} else {
				return o1.get(i).fst - o2.get(i).fst;
			}
		}
	}
}
