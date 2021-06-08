package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ProgramPart;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo.LoopSegment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.CFGUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.DecisionTree;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import edu.kit.joana.util.Triple;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.*;

public class LoopBody extends ProgramPart {

	private final int level;
	private LoopSegment segment;
	private final Map<Integer, Formula[]> in;
	private final Map<Integer, Formula[]> beforeLoop;
	private final Map<Integer, Formula[]> out;
	private final BiMap<Integer, Integer> resultMapping;
	private DecisionTree<Map<Integer, Formula[]>> outDT;
	private final Map<Integer, Array<? extends Value>> placeholderArrays;
	private final Map<Integer, Formula[][]> placeholderArrayVars;
	private final Map<Integer, LoopIteration> runs;
	private final Method owner;
	private final BasicBlock head;
	private final Set<BasicBlock> blocks;
	private final List<BasicBlock> breaks;
	private Formula jumpOut;

	public LoopBody(Method owner, BasicBlock head) {
		this.level = owner.getCFG().getLevel(head);
		this.owner = owner;
		this.in = new HashMap<>();
		this.out = new HashMap<>();
		this.resultMapping = HashBiMap.create();
		this.runs = new HashMap<>();
		this.beforeLoop = new HashMap<>();
		this.head = head;
		this.blocks = this.owner.getCFG().getBasicBlocksInLoop(head);
		this.breaks = findBreaks();
		this.placeholderArrays = new HashMap<>();
		this.placeholderArrayVars = new HashMap<>();
	}

	public void computeLoopCondition() {
		BasicBlock insideLoopSuccessor = head.succs().stream().filter(blocks::contains).findFirst().get();
		boolean evalTo = head.getTrueTarget() == insideLoopSuccessor.idx();
		this.jumpOut = (evalTo) ? LogicUtil.ff.not(head.getCondExpr()) : head.getCondExpr();
	}

	private List<BasicBlock> findBreaks() {
		List<BasicBlock> breaks = new ArrayList<>();
		for (BasicBlock b : this.blocks) {
			if (b.isCondHeader() && b.succs().stream()
					.anyMatch(succ -> owner.getCFG().getLevel(succ) < owner.getCFG().getLevel(head))) {
				breaks.add(b);
				b.setBreak(true);
			}
		}

		// sort the possible break locations inside the loop according to the block indices
		// earliest possible break should be in front
		//
		// a blocks idx is always greater than that of its predecessors
		// (exceptions iis the back edge to a loop head, but we are not looking at loop heads here, so it should be ok)
		Comparator<BasicBlock> comp = (o1, o2) -> {
			assert (!o1.isDummy() && !o2.isDummy());
			return o1.idx() - o2.idx();
		};
		breaks.sort(comp);
		return breaks;
	}

	/**
	 * Compute dependencies of a value that is defined inside the loop
	 * Make sure to simulate the loop before calling this method!
	 *
	 * Implicitly assumes that the loop body is executed at least once (otherwise this value wouldn't exist)
	 *
	 * @param valNum number of a value that is defined inside the loop
	 * @return Formula, that describes the computed value depending on the # of loop iterations
	 */
	public Formula[] extractInLoopValue(int valNum) {
		Formula[] base = this.owner.getDepsForValue(valNum);
		assert (this.runs.size() > 0);

		Formula[] res = LogicUtil.applySubstitution(base, lastRun().makeSubstitution(this.in, this.placeholderArrayVars));

		for (int i = runs.size() - 2; i > 0; i--) {
			Formula jmpOut = runs.get(i).getJumpOutAfterThisIteration();
			Formula[] iterationVal = LogicUtil.applySubstitution(base, runs.get(i).makeSubstitution(this.in, this.placeholderArrayVars));
			res = LogicUtil.ternaryOp(jmpOut, iterationVal, res);
		}

		return base;
	}

	public Substitution generateInitialValueSubstitution() {
		Substitution s = new Substitution();

		for (int i : this.in.keySet()) {
			for (int j = 0; j < this.in.get(i).length; j++) {
				s.addMapping((Variable) this.in.get(i)[j], this.beforeLoop.get(i)[j]);
			}
		}
		return s;
	}

	/**
	 * Simulates the execution of the loop
	 *
	 * @param previous previous run. For the first loop iteration these are the values before the loop is entered
	 * @return Pair, with its first component being a map containg the method's values after the loop execution
	 * and the second component being the condition that must be fulfilled, to exit the loop after the simulated iteration
	 */
	public LoopIteration simulateRun(LoopIteration previous) {
		LoopIteration res = new LoopIteration(previous.getIteration() + 1);
		Substitution substituteInputs = previous.makeSubstitution(this.in, this.placeholderArrayVars);

		// primitive values
		for (int i: this.in.keySet()) {
			res.addPrimitiveVal(i, LogicUtil.applySubstitution(getOut(i), substituteInputs));
		}

		// array values
		for (int i: this.placeholderArrays.keySet()) {
			Formula[][] arrayResult = new Formula[this.placeholderArrayVars.get(i).length][this.placeholderArrays.get(i).elementType().bitwidth()];
			for (int j = 0; j < arrayResult.length; j++) {
				arrayResult[j] = LogicUtil.applySubstitution(this.placeholderArrays.get(i).getValueDependencies()[j], substituteInputs);
			}
			res.addArr(i, arrayResult);
		}

		// jump out condition
		Substitution substituteOutputs = res.makeSubstitution(this.in, this.placeholderArrayVars);
		res.setJumpOutAfterThisIteration(LogicUtil.applySubstitution(this.getJumpOut(), substituteOutputs));
		res.setJumpOutOnlyLoopCondition(LogicUtil.applySubstitution(this.jumpOut, substituteOutputs));

		return res;
	}

	private Formula[] getOut(int valnum) {
		Formula[] mappedTo = this.out.get(valnum);

		Formula wasCalculated;
		if (!owner.isConstant(this.resultMapping.get(valnum))) {
			BasicBlock definedIn = this.blocks.stream().filter(b -> b.ownsValue(resultMapping.get(valnum))).findFirst()
					.get();
			wasCalculated = definedIn.generateImplicitFlowFormula();
		} else {
			wasCalculated = LogicUtil.ff.constant(true);
		}
		mappedTo = LogicUtil.ternaryOp(wasCalculated, mappedTo, this.in.get(valnum));
		return mappedTo;
	}

	public void createPlaceholderArray(int valNum) {
		assert(owner.hasValue(valNum));

		Array<? extends Value> original = (Array<? extends Value>) owner.getValue(valNum);
		try {
			Array<? extends Value> placeHolder = Array.newArray(original.elementType(), valNum, true);
			Formula[][] vars = placeHolder.getValueDependencies();
			this.placeholderArrayVars.put(valNum, vars.clone());
			this.placeholderArrays.put(valNum, placeHolder);
		} catch (UnexpectedTypeException e) {
			e.printStackTrace();
		}
	}

	public Array<? extends Value> getPlaceholderArray(int valNum) {
		if (!this.placeholderArrays.containsKey(valNum)) {
			createPlaceholderArray(valNum);
		}
		return this.placeholderArrays.get(valNum);
	}

	public void addIteration(LoopIteration iteration) {
		this.runs.put(iteration.getIteration(), iteration);
	}

	public LoopIteration getRun(int i) {
		return this.runs.get(i);
	}

	public List<Integer> getAllUses() {
		Set<Integer> usedValues = new HashSet<>();
		this.blocks.forEach(b -> b.instructions().forEach(i -> {
			for (int j = 0; j < i.getNumberOfUses(); j++) {
				usedValues.add(i.getUse(j));
			}
		}));
		return new ArrayList<>(usedValues);
	}

	public List<Integer> getAllDefs() {
		Set<Integer> defs = new HashSet<>();
		this.blocks.forEach(b -> b.instructions().forEach(i -> {
			if (i.hasDef()) {
				defs.add(i.getDef());
			}
		}));
		return new ArrayList<>(defs);
	}

	public List<Integer> getAllWrittenToArrays() {
		Set<Integer> arrs = new HashSet<>();
		this.blocks.forEach(
				b -> b.instructions().stream().filter(i -> i instanceof SSAArrayStoreInstruction).forEach(i -> {
					arrs.add(((SSAArrayStoreInstruction) i).getArrayRef());
				}));
		return new ArrayList<>(arrs);
	}

	public List<Integer> getAllDeclaredArrays() {
		Set<Integer> arrs = new HashSet<>();
		this.blocks.forEach(b -> b.instructions().stream()
				.filter(i -> i instanceof SSANewInstruction && ((SSANewInstruction) i).getConcreteType().isArrayType())
				.forEach(i -> {
					arrs.add(i.getDef());
				}));
		return new ArrayList<>(arrs);
	}

	public Set<BasicBlock> getBlocks() {
		return blocks;
	}

	public boolean hasBlock(int idx) {
		return this.blocks.stream().anyMatch(b -> b.idx() == idx);
	}

	public BasicBlock getHead() {
		return head;
	}

	public void addInDeps(int i, Formula[] deps) {
		this.in.put(i, deps);
	}

	public void addOutDeps(int in, int out) {
		this.out.put(in, owner.getDepsForValue(out));
	}

	public void addBeforeLoopDeps(int in, Formula[] deps) {
		this.beforeLoop.put(in, deps);
	}

	public void addResultMapping(int in, int out) {
		this.resultMapping.put(in, out);
	}

	public boolean producesValNum(int valNum) {
		return this.in.containsKey(valNum);
	}

	public Map<Integer, Integer> phiToInsideLoop() {
		Map<Integer, Integer> outMap = new HashMap<>();

		int predNum = (this.hasBlock(this.getHead().preds().get(0).idx())) ? 0 : 1;

		this.head.instructions().stream().filter(i -> i instanceof SSAPhiInstruction)
				.forEach(phi -> outMap.put(phi.getDef(), phi.getUse(predNum)));
		return outMap;
	}

	/* def - beforeLoop - inLoop */
	public List<Triple<Integer, Integer, Integer>> phiMapping() {
		List<Triple<Integer, Integer, Integer>> phiMap = new ArrayList<>();

		int inLoopPredNum = (this.hasBlock(this.getHead().preds().get(0).idx())) ? 1 : 0;

		this.head.instructions().stream().filter(i -> i instanceof SSAPhiInstruction).forEach(phi -> phiMap
				.add(Triple.triple(phi.getDef(), phi.getUse(1 - inLoopPredNum), phi.getUse(inLoopPredNum))));
		return phiMap;
	}

	public List<BasicBlock> breakToPostLoop(BasicBlock breakBlock) {
		BasicBlock afterBreakBlock = breakBlock.succs().stream().filter(b -> !this.hasBlock(b.idx())).findFirst().get();
		BasicBlock postLoopSuccessor = getPostLoopSuccessor(breakBlock);
		List<BasicBlock> bridge = CFGUtil.blocksBetween(afterBreakBlock, postLoopSuccessor);
		bridge.remove(postLoopSuccessor);
		return bridge;
	}

	public BasicBlock getPostLoopSuccessor(BasicBlock breakBlock) {
		BasicBlock afterBreakBlock = breakBlock.succs().stream().filter(b -> !this.hasBlock(b.idx())).findFirst().get();
		BasicBlock postLoopSuccessor = afterBreakBlock;

		while (this.getOwner().getCFG().isDominatedBy(postLoopSuccessor, breakBlock)) {
			postLoopSuccessor = postLoopSuccessor.succs().get(0);
		}
		return postLoopSuccessor;
	}

	public Formula[] getBeforeLoop(int i) {
		return this.beforeLoop.get(i);
	}

	public List<BasicBlock> getBreaks() {
		return this.breaks;
	}

	public Map<Integer, Formula[]> getIn() {
		return in;
	}

	public Formula getJumpOut() {
		BasicBlock inLoopPred = head.preds().stream().filter(pred -> this.hasBlock(pred.idx())).findFirst().get();
		return LogicUtil.ff.or(LogicUtil.ff.not(inLoopPred.generateImplicitFlowFormula()), this.jumpOut);
	}

	public Method getOwner() {
		return this.owner;
	}

	public Map<Integer, Array<? extends Value>> getPlaceholderArrays() {
		return placeholderArrays;
	}

	public Map<Integer, Formula[][]> getPlaceholderArrayVars() {
		return this.placeholderArrayVars;
	}

	public LoopIteration beforeLoop() {
		return this.runs.get(0);
	}

	public LoopIteration lastRun() {
		return this.runs.get(this.runs.size() - 1);
	}

	public int getSimulatedIterationNum() {
		return this.runs.size();
	}

	public void setOutDT(DecisionTree<Map<Integer, Formula[]>> decisionTree) {
		this.outDT = decisionTree;
	}

	public Formula getJumpOutOnlyLoopCondition() {
		return this.jumpOut;
	}

	public BiMap<Integer, Integer> getResultMapping() {
		return resultMapping;
	}

	public LoopSegment getSegment() {
		return segment;
	}

	public void setSegment(LoopSegment segment) {
		this.segment = segment;
	}
}